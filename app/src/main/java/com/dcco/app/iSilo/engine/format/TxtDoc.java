package com.dcco.app.iSilo.engine.format;

import com.dcco.app.iSilo.engine.util.ErrorUtil;

public class TxtDoc extends DocFormat {

    private byte[] fullText;

    @Override
    public int open(byte[] data, int size) {
        info.formatVersion = 0;
        info.docVersion = 0;
        info.compressed = false;
        info.compressionType = iSiloDocInfo.COMPRESSION_NONE;

        fullText = new byte[size];
        System.arraycopy(data, 0, fullText, 0, size);
        info.textSize = size;

        return 0;
    }

    public int openWithRecords(byte[][] dataRecords, int recordCount) {
        info.formatVersion = 0;
        info.docVersion = 0;
        info.compressed = false;
        info.compressionType = iSiloDocInfo.COMPRESSION_NONE;

        int totalSize = 0;
        for (int i = 0; i < recordCount; i++) {
            if (dataRecords[i] != null) {
                totalSize += dataRecords[i].length;
            }
        }

        fullText = new byte[totalSize];
        int dstPos = 0;
        for (int i = 0; i < recordCount; i++) {
            if (dataRecords[i] != null && dataRecords[i].length > 0) {
                System.arraycopy(dataRecords[i], 0, fullText, dstPos, dataRecords[i].length);
                dstPos += dataRecords[i].length;
            }
        }

        info.textSize = totalSize;
        return 0;
    }

    @Override
    public int getText(int offset, int length, byte[] buffer) {
        if (fullText == null) return ERR_UNSUPPORTED;
        if (offset >= fullText.length) return ERR_END_OF_TEXT;

        int avail = fullText.length - offset;
        if (length > avail) length = avail;
        System.arraycopy(fullText, offset, buffer, 0, length);
        return length;
    }

    @Override
    public int findString(String query, int startOffset, int[] matchOffset, int[] matchLength) {
        if (fullText == null || query == null || query.isEmpty()) return ERR_NOT_FOUND;

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

    @Override
    public int close() {
        fullText = null;
        return 0;
    }
}
