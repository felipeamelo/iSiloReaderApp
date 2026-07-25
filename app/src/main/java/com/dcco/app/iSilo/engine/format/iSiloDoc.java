package com.dcco.app.iSilo.engine.format;

import com.dcco.app.iSilo.engine.PalmDB;
import com.dcco.app.iSilo.engine.render.StyleTableParser;
import com.dcco.app.iSilo.engine.util.AppLog;
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
    private ArrayList<byte[]> decStyleData;
    private int totalDecompressed;

    @Override
    public int open(byte[] record0Data, int record0Size) {
        AppLog.add("ISILODOC_OPEN", "record0Size=%d", record0Size);
        AppLog.hex("ISILODOC_RECORD0", record0Data, 0, Math.min(record0Size, 128));
        silxHeader = new SilXHeader();
        int res = silxHeader.parse(record0Data, 0, record0Size);
        if (ErrorUtil.isError(res)) {
            AppLog.add("ISILODOC_OPEN", "FAILED res=%d", res);
            return res;
        }
        silxHeader.fillInfo(info);
        AppLog.add("ISILODOC_OPEN", "OK info.textSize=%d", info.textSize);
        AppLog.add("ISILODOC_OPEN", "info.formatVersion=%d docVersion=0x%04x", info.formatVersion, info.docVersion);
        return 0;
    }

    public int openWithRecords(byte[] record0Data, int record0Size,
                                byte[][] dataRecords, int recordCount) {
        AppLog.add("OPEN_WITH_RECS", "record0Size=%d recordCount=%d", record0Size, recordCount);
        int res = open(record0Data, record0Size);
        if (ErrorUtil.isError(res)) return res;

        this.rawRecords = dataRecords;
        this.rawRecordCount = recordCount;
        this.decRecords = new ArrayList<>();
        this.decRecordSizes = new ArrayList<>();
        this.decStyleData = new ArrayList<>();
        this.totalDecompressed = 0;

        fullText = new byte[0];
        info.textSize = 0;

        extractLinks();
        extractStyleTable(record0Data, record0Size);
        preloadFirstRecord();
        return 0;
    }

    private void extractLinks() {
        if (info == null) return;
        info.links = new java.util.ArrayList<>();
        AppLog.add("LINKS", "no link records parsed (TOC-based navigation)");
    }

    private void extractStyleTable(byte[] record0Data, int record0Size) {
        if (silxHeader == null || info == null) return;
        int a0 = (info.groupA != null && info.groupA.length > 0) ? info.groupA[0] : 0;
        if (a0 <= 0) {
            AppLog.add("STYLE", "A[0] = 0, no style table");
            return;
        }

        byte[] styleRec = null;
        int styleRecSize = 0;

        if (a0 == 0 && record0Data != null) {
            styleRec = record0Data;
            styleRecSize = record0Size;
        } else if (rawRecords != null && a0 - 1 < rawRecordCount) {
            styleRec = rawRecords[a0 - 1];
            styleRecSize = (styleRec != null) ? styleRec.length : 0;
        }

        if (styleRec == null || styleRecSize < 4) {
            AppLog.add("STYLE", "style record not found at A[0]=%d", a0);
            return;
        }

        AppLog.hex("STYLE_REC", styleRec, 0, Math.min(styleRecSize, 64));

        StyleTableParser.Result styleRes = StyleTableParser.parse(
                styleRec, styleRecSize,
                silxHeader.headerSize,
                a0, silxHeader.bodyFontSize,
                silxHeader.fontAttr1, silxHeader.fontAttr2);

        if (styleRes.fontTable != null) {
            info.fontTable = styleRes.fontTable;
        }
        if (styleRes.styleData != null && styleRes.styleData.length > 0) {
            info.styleData = styleRes.styleData;
            AppLog.add("STYLE", "style data: %d bytes (%d spans)",
                    styleRes.styleData.length, styleRes.styleData.length / 4);
        }
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

        AppLog.add("DECOMPRESS", "lazy rec=%d size=%d", ri, rd.length);

        int totalWords = rd.length / 4;
        int offWords = 9;
        int byteOff = 36;

        if (byteOff >= rd.length) {
            offWords = 0;
            byteOff = 0;
        }

        int availWords = totalWords - offWords;
        if (availWords < 2) return false;

        int blockUnitSize = (silxHeader != null) ? silxHeader.blockUnitSize : 8;
        if (blockUnitSize <= 0) blockUnitSize = 8;

        byte[] flagBytes = extractFlagBytes(rd, rd.length, blockUnitSize);

        ByteArrayOutputStream trial = new ByteArrayOutputStream(65536);
        java.util.ArrayList<Integer> blockSizes = new java.util.ArrayList<>();
        boolean ok = tryDecompressAll(rd, byteOff, availWords, trial, blockSizes);

        if (ok && trial.size() > 0) {
            byte[] dec = trial.toByteArray();
            byte[] styleSpanData = buildStyleSpans(blockSizes, flagBytes);
            decRecords.add(dec);
            decRecordSizes.add(dec.length);
            decStyleData.add(styleSpanData);
            totalDecompressed += dec.length;
            AppLog.add("DECOMPRESS", "  lazy rec=%d: %d bytes spans=%d", ri, dec.length, styleSpanData.length / 4);
            if (decRecords.size() == 1) {
                AppLog.hex("DECOMPRESS_TEXTHEAD", dec, 0, Math.min(30, dec.length));
            }
            return true;
        }

        for (offWords = 0; offWords <= Math.min(totalWords - 2, 10); offWords++) {
            byteOff = offWords * 4;
            availWords = totalWords - offWords;
            if (availWords < 2) break;

            trial = new ByteArrayOutputStream(65536);
            blockSizes.clear();
            ok = tryDecompressAll(rd, byteOff, availWords, trial, blockSizes);
            if (ok && trial.size() > 0) {
                byte[] dec = trial.toByteArray();
                int score = scoreOutput(dec, Math.min(dec.length, 500));
                if (score > 30) {
                    byte[] styleSpanData = buildStyleSpans(blockSizes, flagBytes);
                    decRecords.add(dec);
                    decRecordSizes.add(dec.length);
                    decStyleData.add(styleSpanData);
                    totalDecompressed += dec.length;
                    AppLog.add("DECOMPRESS", "  lazy rec=%d: off=%d score=%d %d bytes spans=%d",
                            ri, byteOff, score, dec.length, styleSpanData.length / 4);
                    if (decRecords.size() == 1) {
                        AppLog.hex("DECOMPRESS_TEXTHEAD", dec, 0, Math.min(30, dec.length));
                    }
                    return true;
                }
            }
        }

        return false;
    }

    private byte[] extractFlagBytes(byte[] recordData, int recordSize, int blockUnitSize) {
        int flagsStart = 4 + (blockUnitSize + 2) * 2;
        if (flagsStart + blockUnitSize > recordSize) {
            flagsStart = 24;
            if (flagsStart + blockUnitSize > recordSize) {
                return new byte[0];
            }
        }
        byte[] flags = new byte[blockUnitSize];
        for (int i = 0; i < blockUnitSize && flagsStart + i < recordSize; i++) {
            flags[i] = recordData[flagsStart + i];
        }
        return flags;
    }

    private byte[] buildStyleSpans(java.util.ArrayList<Integer> blockSizes, byte[] flagBytes) {
        if (blockSizes == null || blockSizes.isEmpty() || flagBytes == null || flagBytes.length == 0) {
            return new byte[0];
        }

        java.util.ArrayList<byte[]> spans = new java.util.ArrayList<>();
        int charOffset = 0;

        for (int bi = 0; bi < blockSizes.size() && bi < flagBytes.length; bi++) {
            int bSize = blockSizes.get(bi);
            if (bSize <= 0) continue;

            int flag = flagBytes[bi] & 0xFF;
            int styleId = flagToStyleId(flag);

            byte[] span = new byte[4];
            span[0] = (byte) ((charOffset >> 8) & 0xFF);
            span[1] = (byte) (charOffset & 0xFF);
            span[2] = (byte) ((styleId >> 8) & 0xFF);
            span[3] = (byte) (styleId & 0xFF);
            spans.add(span);

            charOffset += bSize;
        }

        if (spans.isEmpty()) return new byte[0];
        byte[] result = new byte[spans.size() * 4];
        for (int i = 0; i < spans.size(); i++) {
            byte[] sp = spans.get(i);
            System.arraycopy(sp, 0, result, i * 4, 4);
        }
        return result;
    }

    private int flagToStyleId(int flag) {
        if ((flag & 0x10) != 0 && (flag & 0x20) != 0 && (flag & 0x40) != 0) return 3;
        if ((flag & 0x10) != 0 && (flag & 0x20) != 0) return 3;
        if ((flag & 0x10) != 0 && (flag & 0x40) != 0) return 3;
        if ((flag & 0x20) != 0 && (flag & 0x40) != 0) return 3;
        if ((flag & 0x10) != 0) return 1;
        if ((flag & 0x20) != 0) return 2;
        if ((flag & 0x40) != 0) return 3;
        return 0;
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

        int totalSpans = 0;
        for (int i = 0; i < decStyleData.size(); i++) {
            totalSpans += decStyleData.get(i).length;
        }
        if (totalSpans > 0) {
            byte[] merged = new byte[totalSpans];
            int sp = 0;
            int charOffAdjust = 0;
            for (int i = 0; i < decStyleData.size(); i++) {
                byte[] sd = decStyleData.get(i);
                if (sd == null || sd.length == 0) {
                    charOffAdjust += decRecordSizes.get(i);
                    continue;
                }
                for (int j = 0; j < sd.length; j += 4) {
                    int spanOff = ((sd[j] & 0xFF) << 8) | (sd[j + 1] & 0xFF);
                    spanOff += charOffAdjust;
                    merged[sp] = (byte) ((spanOff >> 8) & 0xFF);
                    merged[sp + 1] = (byte) (spanOff & 0xFF);
                    merged[sp + 2] = sd[j + 2];
                    merged[sp + 3] = sd[j + 3];
                    sp += 4;
                }
                charOffAdjust += decRecordSizes.get(i);
            }
            info.styleData = merged;
            AppLog.add("STYLE_MERGE", "merged %d span bytes across %d records",
                    totalSpans, decRecords.size());
        }

        AppLog.add("DECOMPRESS_ALL", "total=%d bytes (%d records) decStyleData=%d mergedSpans=%d",
                total, decRecords.size(), decStyleData.size(), totalSpans);
    }

    private static boolean tryDecompressAll(byte[] data, int off, int words,
                                            ByteArrayOutputStream output,
                                            java.util.ArrayList<Integer> blockSizes) {
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
                if (blockSizes != null) blockSizes.add(result[0]);
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
