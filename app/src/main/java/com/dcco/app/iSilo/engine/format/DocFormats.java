package com.dcco.app.iSilo.engine.format;

import com.dcco.app.iSilo.engine.PalmDB;
import com.dcco.app.iSilo.engine.util.AppLog;
import com.dcco.app.iSilo.engine.util.ErrorUtil;

public class DocFormats {

    public static String lastDiagnostic = "";

    public static final byte[] TYPE_SDOC  = {'S', 'D', 'o', 'c'};
    public static final byte[] TYPE_TOGO  = {'T', 'o', 'G', 'o'};
    public static final byte[] TYPE_TEXTr = {'T', 'E', 'X', 't'};
    public static final byte[] CREATOR_SiLX = {'S', 'i', 'l', 'X'};
    public static final byte[] CREATOR_Silo = {'S', 'i', 'l', 'o'};

    public static final int FORMAT_UNKNOWN = 0;
    public static final int FORMAT_ISILO1 = 1;
    public static final int FORMAT_ISILO2 = 2;
    public static final int FORMAT_ISILO3 = 3;
    public static final int FORMAT_ISILO4 = 4;
    public static final int FORMAT_DOC = 5;
    public static final int FORMAT_TXT = 6;

    private static final int ERR_CORRUPT = -2028929015;

    public static int identifyFormat(PalmDB pdb) {
        byte[] type = new byte[4];
        byte[] creator = new byte[4];
        int res = pdb.GetInfo(null, creator, type, null, null, null);
        AppLog.add("IDENTIFY", "GetInfo res=%d type=%s creator=%s",
                res, new String(type), new String(creator));
        if (ErrorUtil.isError(res)) {
            lastDiagnostic = "GET_INFO_FAILED err=" + res;
            return FORMAT_UNKNOWN;
        }

        AppLog.add("IDENTIFY", "type=%s creator=%s", new String(type), new String(creator));
        if (arrayEquals(type, TYPE_SDOC)) {
            int[] versionOut = new int[1];
            int[] flagsOut = new int[1];
            res = getISiloVersion(pdb, versionOut, flagsOut);
            if (ErrorUtil.isError(res)) {
                AppLog.add("IDENTIFY", "getISiloVersion FAILED res=%d fallback ISILO3", res);
                return FORMAT_ISILO3;
            }
            AppLog.add("IDENTIFY", "version=0x%04x flags=0x%04x", versionOut[0], flagsOut[0]);
            int version = versionOut[0];
            if (version >= 3) return FORMAT_ISILO3;
            if (version >= 2) return FORMAT_ISILO2;
            return FORMAT_ISILO1;
        }

        if (arrayEquals(type, TYPE_TOGO)) {
            return FORMAT_ISILO2;
        }

        if (arrayEquals(type, TYPE_TEXTr)) {
            return FORMAT_DOC;
        }

        lastDiagnostic = "TYPE=" + new String(type) + " CREATOR=" + new String(creator);
        return FORMAT_UNKNOWN;
    }

    public static DocFormat openFormat(PalmDB pdb) {
        int format = identifyFormat(pdb);
        AppLog.add("OPEN_FORMAT", "format=%d", format);
        if (format == FORMAT_UNKNOWN) return null;

        int[] recordCount = new int[1];
        int res = pdb.GetInfo(null, null, null, recordCount, null, null);
        AppLog.add("OPEN_FORMAT", "GetInfo recordCount=%d", recordCount[0]);
        if (ErrorUtil.isError(res)) {
            lastDiagnostic = "GET_RECORD_COUNT_FAILED err=" + res;
            return null;
        }
        if (recordCount[0] == 0) {
            lastDiagnostic = "RECORD_COUNT_IS_ZERO";
            return null;
        }

        switch (format) {
            case FORMAT_ISILO3:
            case FORMAT_ISILO4:
                return openISiloDoc(pdb, recordCount[0]);
            case FORMAT_ISILO1:
            case FORMAT_ISILO2:
                return openISilo2Doc(pdb, recordCount[0]);
            case FORMAT_DOC:
                return openDocDoc(pdb, recordCount[0]);
            case FORMAT_TXT:
                return openTxtDoc(pdb, recordCount[0]);
            default:
                return null;
        }
    }

    private static DocFormat openISiloDoc(PalmDB pdb, int recordCount) {
        byte[][] record0 = new byte[1][];
        int[] size0 = new int[1];
        int res = pdb.GetRecord(0, size0, record0);
        AppLog.add("OPEN_ISILO_DOC", "GetRecord(0) res=%d size=%d", res, size0[0]);
        if (ErrorUtil.isError(res)) {
            lastDiagnostic = "GET_RECORD0 err=" + res;
            return null;
        }

        iSiloDoc doc = new iSiloDoc();
        if (recordCount > 1) {
            AppLog.add("OPEN_ISILO_DOC", "loadRecords start=1 count=%d", recordCount - 1);
            byte[][] records = loadRecords(pdb, 1, recordCount - 1);
            if (records == null) {
                lastDiagnostic = "LOAD_RECORDS_NULL";
                return null;
            }
            AppLog.add("OPEN_ISILO_DOC", "records loaded=%d", records.length);
            for (int i = 0; i < records.length; i++) {
                AppLog.add("OPEN_ISILO_DOC", "  records[%d] size=%d", i, records[i] != null ? records[i].length : -1);
            }
            res = doc.openWithRecords(record0[0], size0[0], records, recordCount - 1);
        } else {
            AppLog.add("OPEN_ISILO_DOC", "single record, no text records");
            res = doc.open(record0[0], size0[0]);
        }
        AppLog.add("OPEN_ISILO_DOC", "openWithRecords res=%d", res);
        if (ErrorUtil.isError(res)) {
            lastDiagnostic = "OPEN_ISILO err=" + res;
            return null;
        }

        byte[] nameBuf = new byte[32];
        res = pdb.GetInfo(nameBuf, null, null, null, null, null);
        if (!ErrorUtil.isError(res) && doc.getInfo() != null) {
            int len = 0;
            while (len < 32 && nameBuf[len] != 0) len++;
            if (len > 0) {
                doc.getInfo().title = new String(nameBuf, 0, len);
                AppLog.add("OPEN_ISILO_DOC", "title='%s'", doc.getInfo().title);
            }
        }

        extractPageOffsets(pdb, doc);
        extractTOC(pdb, doc);
        return doc;
    }

    private static void extractTOC(PalmDB pdb, iSiloDoc doc) {
        iSiloDocInfo info = doc.getInfo();
        if (info == null) return;

        java.util.ArrayList<String> titles = new java.util.ArrayList<>();
        java.util.ArrayList<Integer> offsets = new java.util.ArrayList<>();

        int[] recCount = new int[1];
        int res = pdb.GetInfo(null, null, null, recCount, null, null);
        int totalRecords = (!ErrorUtil.isError(res) && recCount[0] > 0) ? recCount[0] : 0;

        for (int ri = 1; ri < totalRecords; ri++) {
            byte[][] recData = new byte[1][];
            int[] recSize = new int[1];
            res = pdb.GetRecord(ri, recSize, recData);
            if (ErrorUtil.isError(res) || recData[0] == null || recSize[0] < 6) continue;

            byte[] rd = recData[0];
            if ((rd[0] & 0xFF) != 0x04) continue;
            int sub = rd[1] & 0xFF;
            if (sub != 7 && sub != 8) continue;

            AppLog.add("TOC_REC", "ri=%d sub=%d size=%d", ri, sub, recSize[0]);

            if (sub == 8) {
                int pos = 2;
                while (pos + 4 < recSize[0]) {
                    int off = read16(rd, pos);
                    int titleLen = rd[pos + 2] & 0xFF;
                    pos += 4;
                    if (pos + titleLen > recSize[0]) break;
                    if (titleLen > 0) {
                        StringBuilder sb = new StringBuilder(titleLen);
                        for (int i = 0; i < titleLen; i++) {
                            char c = (char) (rd[pos + i] & 0xFF);
                            if (c == 0) break;
                            sb.append(c);
                        }
                        titles.add(sb.toString());
                        offsets.add(off);
                        AppLog.add("TOC_ENTRY", "  off=%d title='%s'", off, sb.toString());
                    }
                    pos += titleLen;
                }
            }
        }

        if (titles.size() > 0) {
            info.tocTitles = titles.toArray(new String[0]);
            info.tocOffsets = new int[offsets.size()];
            for (int i = 0; i < offsets.size(); i++) {
                info.tocOffsets[i] = offsets.get(i);
            }
            AppLog.add("TOC", "extracted %d entries from sub=7/8 records", titles.size());
        } else {
            int tocRecIdx = info.recordStarts[1];
            int tocRecCount = info.recordCounts[1];
            if (tocRecIdx > 0 && tocRecCount > 0) {
                AppLog.add("TOC", "fallback: recIdx=%d count=%d", tocRecIdx, tocRecCount);
                for (int ri = 0; ri < tocRecCount; ri++) {
                    byte[][] rData = new byte[1][];
                    int[] rSize = new int[1];
                    res = pdb.GetRecord(tocRecIdx + ri, rSize, rData);
                    if (ErrorUtil.isError(res) || rData[0] == null || rSize[0] < 6) continue;
                    byte[] rd = rData[0];
                    int pos = 0;
                    while (pos + 4 < rSize[0]) {
                        int off = read16(rd, pos);
                        int titleLen = rd[pos + 2] & 0xFF;
                        pos += 4;
                        if (pos + titleLen > rSize[0]) break;
                        if (titleLen > 0) {
                            StringBuilder sb = new StringBuilder(titleLen);
                            for (int i = 0; i < titleLen; i++) {
                                char c = (char) (rd[pos + i] & 0xFF);
                                if (c == 0) break;
                                sb.append(c);
                            }
                            titles.add(sb.toString());
                            offsets.add(off);
                        }
                        pos += titleLen;
                    }
                }
                if (titles.size() > 0) {
                    info.tocTitles = titles.toArray(new String[0]);
                    info.tocOffsets = new int[offsets.size()];
                    for (int i = 0; i < offsets.size(); i++) info.tocOffsets[i] = offsets.get(i);
                    AppLog.add("TOC", "fallback extracted %d entries", titles.size());
                }
            }
        }
    }

    private static void extractPageOffsets(PalmDB pdb, iSiloDoc doc) {
        iSiloDocInfo info = doc.getInfo();
        if (info == null) return;

        java.util.ArrayList<Integer> allOffsets = new java.util.ArrayList<>();

        for (int treeIdx = 0; treeIdx < 2; treeIdx++) {
            int recIdx = info.recordStarts[2 + treeIdx];
            if (recIdx <= 0) continue;

            byte[][] recData = new byte[1][];
            int[] recSize = new int[1];
            int res = pdb.GetRecord(recIdx, recSize, recData);
            if (ErrorUtil.isError(res) || recData[0] == null || recSize[0] < 8) continue;

            byte[] rd = recData[0];
            AppLog.add("PAGE_TREE", "recIdx=%d size=%d", recIdx, recSize[0]);
            AppLog.hex("PAGE_TREE_REC", rd, 0, Math.min(recSize[0], 64));

            int hdrStart = rd[0] & 0xFF;
            if (hdrStart <= 0 || hdrStart >= recSize[0]) continue;

            int d0 = rd[hdrStart] & 0xFF;
            int d3 = (hdrStart + 3 < recSize[0]) ? rd[hdrStart + 3] & 0xFF : 0;
            AppLog.add("PAGE_TREE", "  hdrStart=%d d0=%d d3=%d", hdrStart, d0, d3);

            int tableOff = hdrStart + d0;
            int entryCount = Math.min(d3 + 1, 2);

            for (int ei = 0; ei < entryCount; ei++) {
                if (tableOff + 4 > recSize[0]) break;
                int subOff = read16(rd, tableOff + ei * 4);
                int subCnt = read16(rd, tableOff + ei * 4 + 2);
                AppLog.add("PAGE_TREE", "  sub[%d]: off=%d cnt=%d", ei, subOff, subCnt);
                if (subCnt <= 0 || subOff <= 0 || subOff >= recSize[0]) continue;

                int ebBase = subOff;
                int ebD0 = rd[ebBase] & 0xFF;
                int ebType = rd[ebBase + 2] & 3;
                int ebCount = read16(rd, ebBase + 6);
                int ebLast = read32(rd, ebBase + 12);

                AppLog.add("PAGE_TREE", "    eb: d0=%d type=%d count=%d last=%d",
                        ebD0, ebType, ebCount, ebLast);

                int dataOff = ebBase + ebD0;
                int offset = 0;
                int decoded = 0;
                int extraIdx = ebBase + 16;

                for (int i = 0; i < ebCount && dataOff < recSize[0]; i++) {
                    int delta;
                    if (ebType == 0) {
                        if (dataOff >= recSize[0]) break;
                        delta = (rd[dataOff] & 0xFF) + 1;
                        dataOff++;
                    } else if (ebType == 1) {
                        if (dataOff >= recSize[0]) break;
                        int b = rd[dataOff] & 0xFF;
                        dataOff++;
                        if ((b & 0x80) == 0) {
                            delta = b + 1;
                        } else {
                            delta = (b & 0x7F) + 1;
                            if (extraIdx < recSize[0]) {
                                delta += (rd[extraIdx] & 0xFF) << 7;
                                extraIdx++;
                            }
                        }
                    } else if (ebType == 2) {
                        if (dataOff + 1 >= recSize[0]) break;
                        delta = read16(rd, dataOff) + 1;
                        dataOff += 2;
                    } else if (ebType == 3) {
                        if (dataOff + 1 >= recSize[0]) break;
                        int v = read16(rd, dataOff);
                        dataOff += 2;
                        if ((v & 0x8000) == 0) {
                            delta = v + 1;
                        } else {
                            delta = (v & 0x7FFF) + 1;
                            if (extraIdx + 1 < recSize[0]) {
                                delta += read16(rd, extraIdx) << 15;
                                extraIdx += 2;
                            }
                        }
                    } else {
                        break;
                    }
                    offset += delta;
                    allOffsets.add(offset);
                    decoded++;
                }

                if (decoded > 0) {
                    AppLog.add("PAGE_TREE", "    decoded %d offsets, last=%d", decoded, offset);
                }
            }
        }

        if (allOffsets.size() > 0) {
            info.pageOffsets = new int[allOffsets.size()];
            for (int i = 0; i < allOffsets.size(); i++) {
                info.pageOffsets[i] = allOffsets.get(i);
            }
            AppLog.add("PAGE_TREE", "total pageOffsets=%d", info.pageOffsets.length);
        } else {
            AppLog.add("PAGE_TREE", "no page offsets found, will use fallback");
        }
    }

    private static int read16(byte[] buf, int off) {
        return ((buf[off] & 0xFF) << 8) | (buf[off + 1] & 0xFF);
    }

    private static int read32(byte[] buf, int off) {
        return (buf[off] << 24) | ((buf[off + 1] & 0xFF) << 16)
                | ((buf[off + 2] & 0xFF) << 8) | (buf[off + 3] & 0xFF);
    }

    private static DocFormat openISilo2Doc(PalmDB pdb, int recordCount) {
        byte[][] record0 = new byte[1][];
        int[] size0 = new int[1];
        int res = pdb.GetRecord(0, size0, record0);
        if (ErrorUtil.isError(res)) return null;

        iSilo2Doc doc = new iSilo2Doc();
        if (recordCount > 1) {
            byte[][] records = loadRecords(pdb, 1, recordCount - 1);
            if (records == null) return null;
            res = doc.openWithRecords(record0[0], size0[0], records, recordCount - 1);
        } else {
            res = doc.open(record0[0], size0[0]);
        }
        if (ErrorUtil.isError(res)) return null;
        return doc;
    }

    private static DocFormat openDocDoc(PalmDB pdb, int recordCount) {
        byte[][] record0 = new byte[1][];
        int[] size0 = new int[1];
        int res = pdb.GetRecord(0, size0, record0);
        if (ErrorUtil.isError(res)) return null;

        DocDoc doc = new DocDoc();
        if (recordCount > 1) {
            byte[][] records = loadRecords(pdb, 1, recordCount - 1);
            if (records == null) return null;
            res = doc.openWithRecords(record0[0], size0[0], records, recordCount - 1);
        } else {
            res = doc.open(record0[0], size0[0]);
        }
        if (ErrorUtil.isError(res)) return null;
        return doc;
    }

    private static DocFormat openTxtDoc(PalmDB pdb, int recordCount) {
        TxtDoc doc = new TxtDoc();

        if (recordCount > 0) {
            byte[][] records = loadRecords(pdb, 0, recordCount);
            if (records == null) return null;
            int res = doc.openWithRecords(records, recordCount);
            if (ErrorUtil.isError(res)) return null;
        } else {
            byte[][] record0 = new byte[1][];
            int[] size0 = new int[1];
            int res = pdb.GetRecord(0, size0, record0);
            if (ErrorUtil.isError(res)) return null;
            res = doc.open(record0[0], size0[0]);
            if (ErrorUtil.isError(res)) return null;
        }
        return doc;
    }

    private static byte[][] loadRecords(PalmDB pdb, int startIndex, int count) {
        byte[][] records = new byte[count][];
        for (int i = 0; i < count; i++) {
            int[] size = new int[1];
            byte[][] data = new byte[1][];
            int res = pdb.GetRecord(startIndex + i, size, data);
            if (!ErrorUtil.isError(res) && data[0] != null) {
                records[i] = data[0];
            }
        }
        return records;
    }

    private static int getISiloVersion(PalmDB pdb, int[] versionOut, int[] flagsOut) {
        byte[][] data = new byte[1][];
        int[] size = new int[1];
        int res = pdb.GetRecord(0, size, data);
        AppLog.add("GET_ISILO_VER", "GetRecord(0) res=%d size=%d", res, size[0]);
        if (ErrorUtil.isError(res)) {
            lastDiagnostic = "GET_RECORD0_IN_VERSION err=" + res;
            return res;
        }
        if (size[0] < 4) {
            AppLog.add("GET_ISILO_VER", "record0 too small: %d < 4", size[0]);
            lastDiagnostic = "RECORD0_TOO_SMALL size=" + size[0];
            return ERR_CORRUPT;
        }
        AppLog.hex("GET_ISILO_VER_REC0", data[0], 0, Math.min(size[0], 32));

        versionOut[0] = ((data[0][0] & 0xFF) << 8) | (data[0][1] & 0xFF);
        flagsOut[0] = ((data[0][2] & 0xFF) << 8) | (data[0][3] & 0xFF);
        return 0;
    }

    private static boolean arrayEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }
}
