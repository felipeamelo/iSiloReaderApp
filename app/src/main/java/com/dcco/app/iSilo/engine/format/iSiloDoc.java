package com.dcco.app.iSilo.engine.format;

import com.dcco.app.iSilo.engine.PalmDB;
import com.dcco.app.iSilo.engine.util.DebugLog;
import com.dcco.app.iSilo.engine.util.ErrorUtil;
import com.dcco.app.iSilo.engine.util.HuffmanInflator;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class iSiloDoc extends DocFormat {

    private byte[] fullText;

    private SilXHeader silxHeader;

    private byte[][] rawRecords;
    private int rawRecordCount;

    private ArrayList<byte[]> decRecords;
    private ArrayList<Integer> decRecordSizes;
    private int totalDecompressed;

    @Override
    public int open(byte[] record0Data, int record0Size) {
        DebugLog.add("ISILODOC_OPEN", "record0Size=%d", record0Size);
        DebugLog.hex("ISILODOC_RECORD0", record0Data, 0, Math.min(record0Size, 128));
        silxHeader = new SilXHeader();
        int res = silxHeader.parse(record0Data, 0, record0Size);
        if (ErrorUtil.isError(res)) {
            DebugLog.add("ISILODOC_OPEN", "FAILED res=%d", res);
            return res;
        }
        silxHeader.fillInfo(info);
        DebugLog.add("ISILODOC_OPEN", "OK info.textSize=%d", info.textSize);
        DebugLog.add("ISILODOC_OPEN", "info.formatVersion=%d docVersion=0x%04x", info.formatVersion, info.docVersion);
        return 0;
    }

    public int openWithRecords(byte[] record0Data, int record0Size,
                                byte[][] dataRecords, int recordCount) {
        DebugLog.add("OPEN_WITH_RECS", "record0Size=%d recordCount=%d", record0Size, recordCount);
        int res = open(record0Data, record0Size);
        if (ErrorUtil.isError(res)) return res;

        this.rawRecords = dataRecords;
        this.rawRecordCount = recordCount;
        this.decRecords = new ArrayList<>();
        this.decRecordSizes = new ArrayList<>();
        this.totalDecompressed = 0;

        fullText = new byte[0];
        info.textSize = 0;

        extractLinks();
        preloadFirstRecord();
        return 0;
    }

    private void extractLinks() {
        if (info == null) return;
        info.links = new java.util.ArrayList<>();

        for (int ri = 0; ri < rawRecordCount; ri++) {
            if (rawRecords[ri] == null || rawRecords[ri].length < 8) continue;
            byte[] rd = rawRecords[ri];
            int recType = rd[0] & 0xFF;
            int recSub = rd[1] & 0xFF;
            if (recType != 0x04 || recSub != 0x01) continue;

            DebugLog.hex("LINK_REC" + ri, rd, 0, Math.min(rd.length, 64));
            DebugLog.add("LINK_REC" + ri, "size=%d", rd.length);

            int pos = 2;
            while (pos + 3 < rd.length) {
                int entryType = rd[pos] & 0xFF;
                if (entryType == 0) break;
                int val = ((rd[pos + 1] & 0xFF) << 8) | (rd[pos + 2] & 0xFF);
                DebugLog.add("LINK_ENTRY", "  rec=%d pos=%d type=0x%02x val=%d", ri, pos, entryType, val);
                pos += 3;
            }
        }
        DebugLog.add("LINKS", "found %d link records", info.links.size());
    }

    private void preloadFirstRecord() {
        for (int ri = 0; ri < rawRecordCount; ri++) {
            if (ensureRecordDecompressed(ri)) {
                DocFormats.lastDiagnostic = "preloaded rec=" + ri + " size=" + decRecordSizes.get(decRecordSizes.size() - 1);
                break;
            }
        }
        rebuildFullText();
    }

    private boolean ensureRecordDecompressed(int ri) {
        if (ri >= rawRecordCount || rawRecords[ri] == null) return false;

        byte[] rd = rawRecords[ri];
        int recType = rd[0] & 0xFF;
        int recSub = rd[1] & 0xFF;
        if (recType != 0x04 || recSub != 0x00) return false;

        DebugLog.add("DECOMPRESS", "lazy rec=%d size=%d", ri, rd.length);

        int totalWords = rd.length / 4;
        int offWords = 9;
        int byteOff = 36;

        if (byteOff >= rd.length) {
            offWords = 0;
            byteOff = 0;
        }

        int availWords = totalWords - offWords;
        if (availWords < 2) return false;

        ByteArrayOutputStream trial = new ByteArrayOutputStream(65536);
        boolean ok = tryDecompressAll(rd, byteOff, availWords, trial);
        if (ok && trial.size() > 0) {
            byte[] dec = trial.toByteArray();
            decRecords.add(dec);
            decRecordSizes.add(dec.length);
            totalDecompressed += dec.length;
            DebugLog.add("DECOMPRESS", "  lazy rec=%d: %d bytes", ri, dec.length);
            if (decRecords.size() == 1) {
                DebugLog.hex("DECOMPRESS_TEXTHEAD", dec, 0, Math.min(30, dec.length));
            }
            return true;
        }

        for (offWords = 0; offWords <= Math.min(totalWords - 2, 10); offWords++) {
            byteOff = offWords * 4;
            availWords = totalWords - offWords;
            if (availWords < 2) break;

            trial = new ByteArrayOutputStream(65536);
            ok = tryDecompressAll(rd, byteOff, availWords, trial);
            if (ok && trial.size() > 0) {
                byte[] dec = trial.toByteArray();
                int score = scoreOutput(dec, Math.min(dec.length, 500));
                if (score > 30) {
                    decRecords.add(dec);
                    decRecordSizes.add(dec.length);
                    totalDecompressed += dec.length;
                    DebugLog.add("DECOMPRESS", "  lazy rec=%d: off=%d score=%d %d bytes", ri, byteOff, score, dec.length);
                    if (decRecords.size() == 1) {
                        DebugLog.hex("DECOMPRESS_TEXTHEAD", dec, 0, Math.min(30, dec.length));
                    }
                    return true;
                }
            }
        }

        return false;
    }

    private void rebuildFullText() {
        int total = 0;
        for (int i = 0; i < decRecordSizes.size(); i++) {
            total += decRecordSizes.get(i);
        }
        if (total <= 0) return;

        fullText = new byte[total];
        int pos = 0;
        for (int i = 0; i < decRecords.size(); i++) {
            byte[] d = decRecords.get(i);
            System.arraycopy(d, 0, fullText, pos, d.length);
            pos += d.length;
        }
        info.textSize = total;
        DebugLog.add("DECOMPRESS_ALL", "total=%d bytes (%d records)", total, decRecords.size());
    }

    private static boolean tryDecompressAll(byte[] data, int off, int words,
                                            ByteArrayOutputStream output) {
        try {
            HuffmanInflator inflator = new HuffmanInflator();
            int treeRes = inflator.GetTrees(data, off, words, 0);
            if (ErrorUtil.isError(treeRes)) return false;
            int consumed = inflator.getBytesConsumed();
            int blockPos = off + consumed;
            int remWords = words - consumed / 4;
            if (remWords <= 0) return false;

            int blockCount = 0;
            int maxBlocks = 32;
            while (remWords > 0 && blockCount < maxBlocks) {
                byte[] buf = new byte[65536];
                int[] result = new int[1];
                int inflateRes = inflator.InflateBlock(data, blockPos, remWords,
                        buf, 0, buf.length, result);
                if (ErrorUtil.isError(inflateRes)) break;
                if (result[0] <= 0) break;

                output.write(buf, 0, result[0]);
                blockCount++;

                int blockBytes = inflator.getBytesConsumed();
                if (blockBytes <= 0) break;
                blockPos += blockBytes;
                remWords -= blockBytes / 4;
            }
            return blockCount > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static int scoreOutput(byte[] data, int len) {
        int check = Math.min(len, 500);
        if (check < 50) return -1;
        int printable = 0, spaces = 0, newlines = 0;
        for (int i = 0; i < check; i++) {
            int b = data[i] & 0xFF;
            if (b >= 0x20 && b <= 0x7E) printable++;
            if (b == 0x20) spaces++;
            if (b == 0x0A || b == 0x0D) newlines++;
        }
        return printable * 100 / check + spaces * 10 / check + newlines * 20 / check;
    }

    @Override
    public int getText(int offset, int length, byte[] buffer) {
        if (fullText == null || fullText.length == 0) {
            if (rawRecords != null) {
                for (int ri = 0; ri < rawRecordCount; ri++) {
                    ensureRecordDecompressed(ri);
                }
                rebuildFullText();
            }
        }
        if (fullText == null || fullText.length == 0) return ERR_UNSUPPORTED;
        if (offset >= fullText.length) {
            return ERR_END_OF_TEXT;
        }
        int avail = fullText.length - offset;
        if (length > avail) length = avail;
        System.arraycopy(fullText, offset, buffer, 0, length);
        return length;
    }

    @Override
    public int findString(String query, int startOffset, int[] matchOffset, int[] matchLength) {
        if (fullText == null || fullText.length == 0) {
            if (rawRecords != null) {
                for (int ri = 0; ri < rawRecordCount; ri++) {
                    ensureRecordDecompressed(ri);
                }
                rebuildFullText();
            }
        }
        if (fullText == null || query == null || query.isEmpty()) {
            return ERR_NOT_FOUND;
        }
        byte[] queryBytes;
        try {
            queryBytes = query.getBytes("UTF-8");
        } catch (Exception e) {
            queryBytes = query.getBytes();
        }

        int searchFrom = startOffset < 0 ? 0 : startOffset;
        int maxOffset = fullText.length - queryBytes.length;

        for (int i = searchFrom; i <= maxOffset; i++) {
            boolean found = true;
            for (int j = 0; j < queryBytes.length; j++) {
                if (fullText[i + j] != queryBytes[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                if (matchOffset != null) matchOffset[0] = i;
                if (matchLength != null) matchLength[0] = queryBytes.length;
                return 0;
            }
        }
        return ERR_NOT_FOUND;
    }
}
