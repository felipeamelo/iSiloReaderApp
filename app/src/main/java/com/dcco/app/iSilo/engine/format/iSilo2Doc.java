package com.dcco.app.iSilo.engine.format;

import com.dcco.app.iSilo.engine.util.iSiloDecompress;
import com.dcco.app.iSilo.engine.util.ErrorUtil;

public class iSilo2Doc extends DocFormat {

    private byte[] fullText;

    @Override
    public int open(byte[] data, int size) {
        if (size < 8) return ERR_FILE_CORRUPT;

        int version = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
        int flags = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);

        info.docVersion = version;
        if (version >= 0x0200) {
            info.formatVersion = 2;
        } else {
            info.formatVersion = 1;
        }
        info.compressed = (flags & 0x8000) == 0;
        info.encrypted = (flags & 0x0002) != 0;
        info.hasTOC = (flags & 0x0004) != 0;

        int pos = 4;
        if (pos + 1 <= size) {
            int titleLen = data[pos] & 0xFF;
            pos++;
            if (titleLen > 0 && pos + titleLen <= size) {
                info.title = new String(data, pos, titleLen);
                pos += titleLen;
            }
        }

        if (pos + 1 <= size) {
            int authorLen = data[pos] & 0xFF;
            pos++;
            if (authorLen > 0 && pos + authorLen <= size) {
                info.author = new String(data, pos, authorLen);
                pos += authorLen;
            }
        }

        header = new DocHeader();

        return 0;
    }

    public int openWithRecords(byte[] record0Data, int record0Size,
                                byte[][] dataRecords, int recordCount) {
        int res = open(record0Data, record0Size);
        if (ErrorUtil.isError(res)) return res;

        int totalSize = 0;
        int[] sizes = new int[recordCount];

        for (int i = 0; i < recordCount; i++) {
            if (dataRecords[i] == null || dataRecords[i].length == 0) continue;

            int[] outSize = new int[1];
            res = iSiloDecompress.calcBlockSize(dataRecords[i], 0, dataRecords[i].length, outSize);
            if (ErrorUtil.isError(res)) return res;

            sizes[i] = outSize[0];
            totalSize += outSize[0];
        }

        fullText = new byte[totalSize];
        int dstPos = 0;
        for (int i = 0; i < recordCount; i++) {
            if (dataRecords[i] == null || dataRecords[i].length == 0) continue;

            int[] written = new int[1];
            res = iSiloDecompress.decompressBlock(dataRecords[i], 0, dataRecords[i].length,
                    fullText, dstPos, sizes[i], written);
            if (ErrorUtil.isError(res)) return res;

            dstPos += written[0];
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
}
