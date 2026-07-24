package com.dcco.app.iSilo.engine.format;

import com.dcco.app.iSilo.engine.util.ErrorUtil;

public class DocDoc extends DocFormat {

    private byte[] fullText;

    public static final int COMPRESSION_NONE = 0;
    public static final int COMPRESSION_DOC = 1;
    public static final int COMPRESSION_ZLIB = 2;

    private static final int DOC_HEADER_SIZE = 16;
    private static final int MAX_BLOCK_SIZE = 4096;

    @Override
    public int open(byte[] data, int size) {
        if (size < DOC_HEADER_SIZE) return ERR_FILE_CORRUPT;

        int compression = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
        int textLength = ((data[4] & 0xFF) << 24) | ((data[5] & 0xFF) << 16)
                       | ((data[6] & 0xFF) << 8) | (data[7] & 0xFF);
        int recordCount = ((data[8] & 0xFF) << 8) | (data[9] & 0xFF);
        int recordSize = ((data[10] & 0xFF) << 8) | (data[11] & 0xFF);

        info.formatVersion = 0;
        info.docVersion = 0;
        info.compressed = compression != COMPRESSION_NONE;

        if (compression == COMPRESSION_DOC) {
            info.compressionType = iSiloDocInfo.COMPRESSION_LZ77;
        } else if (compression == COMPRESSION_ZLIB) {
            info.compressionType = iSiloDocInfo.COMPRESSION_HUFFMAN;
        } else {
            info.compressionType = iSiloDocInfo.COMPRESSION_NONE;
        }

        if (size > DOC_HEADER_SIZE) {
            int nameLen = Math.min(size - DOC_HEADER_SIZE, 64);
            info.title = new String(data, DOC_HEADER_SIZE, nameLen).trim();
            int nullPos = info.title.indexOf(0);
            if (nullPos >= 0) info.title = info.title.substring(0, nullPos);
        }

        if (recordSize == 0) recordSize = MAX_BLOCK_SIZE;

        header = new DocHeader();
        info.textSize = textLength;
        return 0;
    }

    public int openWithRecords(byte[] headerData, int headerSize,
                                byte[][] dataRecords, int recordCount) {
        int res = open(headerData, headerSize);
        if (ErrorUtil.isError(res)) return res;

        int compression = ((headerData[0] & 0xFF) << 8) | (headerData[1] & 0xFF);

        int totalSize = info.textSize > 0 ? info.textSize : estimateTotalSize(dataRecords, recordCount);
        fullText = new byte[totalSize];
        int dstPos = 0;

        for (int i = 0; i < recordCount; i++) {
            if (dataRecords[i] == null || dataRecords[i].length == 0) continue;

            if (compression == COMPRESSION_NONE) {
                int copyLen = dataRecords[i].length;
                if (dstPos + copyLen > totalSize) copyLen = totalSize - dstPos;
                System.arraycopy(dataRecords[i], 0, fullText, dstPos, copyLen);
                dstPos += copyLen;
            } else if (compression == COMPRESSION_DOC) {
                int[] written = new int[1];
                decompressDocBlock(dataRecords[i], 0, dataRecords[i].length,
                        fullText, dstPos, totalSize - dstPos, written);
                dstPos += written[0];
            } else {
                return ERR_UNSUPPORTED;
            }
        }

        info.textSize = dstPos;
        return 0;
    }

    private int estimateTotalSize(byte[][] records, int count) {
        int total = 0;
        for (int i = 0; i < count; i++) {
            if (records[i] != null) {
                total += records[i].length * 2;
            }
        }
        return total;
    }

    private void decompressDocBlock(byte[] in, int inOff, int inLen,
                                     byte[] out, int outOff, int maxOut,
                                     int[] bytesWritten) {
        int ip = inOff;
        int end = inOff + inLen;
        int op = 0;

        while (ip < end && op < maxOut) {
            int token = in[ip] & 0xFF;
            ip++;

            if (token == 0) {
                out[outOff + op++] = 0;
            } else if (token < 0x09) {
                int count = token;
                if (ip + count > end) count = end - ip;
                if (op + count > maxOut) count = maxOut - op;
                System.arraycopy(in, ip, out, outOff + op, count);
                ip += count;
                op += count;
            } else if (token < 0x80) {
                out[outOff + op++] = (byte) token;
            } else if (token < 0xC0) {
                if (ip >= end) break;
                int next = in[ip] & 0xFF;
                ip++;
                int offset = ((token & 0x3F) << 8) | next;
                int length = 3;
                if (offset > op) offset = op;
                if (op + length > maxOut) length = maxOut - op;
                for (int i = 0; i < length; i++) {
                    out[outOff + op] = out[outOff + op - offset];
                    op++;
                }
            } else {
                if (ip >= end) break;
                int next = in[ip] & 0xFF;
                ip++;
                int offset = ((token & 0x3F) << 8) | next;
                int length = ((token >> 2) & 7) + 3;
                if (offset > op) offset = op;
                if (op + length > maxOut) length = maxOut - op;
                for (int i = 0; i < length; i++) {
                    out[outOff + op] = out[outOff + op - offset];
                    op++;
                }
            }
        }

        bytesWritten[0] = op;
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
