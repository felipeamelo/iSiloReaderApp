package com.dcco.app.iSilo.engine.format;

import com.dcco.app.iSilo.engine.util.ErrorUtil;

public class DocHeader {

    private int version;
    private int flags;
    private int totalTextSize;
    private int recordCount;
    private int recOffsetBits;
    private int[] pageOffsets;
    private int[] tocOffsets;
    private String[] tocTitles;
    private int imageCount;
    private int[] imageOffsets;

    public DocHeader() {
    }

    public int parse(byte[] data, int offset, int length, iSiloDocInfo info) {
        if (length < 12) return -2028929015;

        int pos = offset;
        version = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
        pos += 2;
        info.docVersion = version;

        int majorVersion = (version >> 8) & 0xFF;
        info.formatVersion = majorVersion;

        flags = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
        pos += 2;

        info.compressed = (flags & 1) == 0;
        info.encrypted = (flags & 2) != 0;
        info.hasTOC = (flags & 4) != 0;
        info.hasImages = (flags & 8) != 0;

        if (majorVersion >= 4) {
            info.compressionType = iSiloDocInfo.COMPRESSION_HUFFMAN;
        } else {
            info.compressionType = iSiloDocInfo.COMPRESSION_LZ77;
        }

        totalTextSize = readInt32(data, pos);
        pos += 4;

        recordCount = readInt16(data, pos);
        pos += 2;

        recOffsetBits = readInt16(data, pos);
        pos += 2;

        int titleLen = data[pos] & 0xFF;
        pos++;
        if (titleLen > 0 && pos + titleLen <= offset + length) {
            info.title = readString(data, pos, titleLen);
            pos += titleLen;
        }

        int authorLen = data[pos] & 0xFF;
        pos++;
        if (authorLen > 0 && pos + authorLen <= offset + length) {
            info.author = readString(data, pos, authorLen);
            pos += authorLen;
        }

        if (version >= 4) {
            int publisherLen = data[pos] & 0xFF;
            pos++;
            if (publisherLen > 0 && pos + publisherLen <= offset + length) {
                info.publisher = readString(data, pos, publisherLen);
                pos += publisherLen;
            }

            int isbnLen = data[pos] & 0xFF;
            pos++;
            if (isbnLen > 0 && pos + isbnLen <= offset + length) {
                info.isbn = readString(data, pos, isbnLen);
                pos += isbnLen;
            }

            int copyrightLen = data[pos] & 0xFF;
            pos++;
            if (copyrightLen > 0 && pos + copyrightLen <= offset + length) {
                info.copyright = readString(data, pos, copyrightLen);
                pos += copyrightLen;
            }

            int descLen = data[pos] & 0xFF;
            pos++;
            if (descLen > 0 && pos + descLen <= offset + length) {
                info.description = readString(data, pos, descLen);
                pos += descLen;
            }
        }

        if (info.hasTOC && pos + 4 <= offset + length) {
            int tocCount = readInt16(data, pos);
            pos += 2;
            int tocLen = readInt16(data, pos);
            pos += 2;

            if (tocCount > 0 && tocLen > 0 && pos + tocLen <= offset + length) {
                tocOffsets = new int[tocCount];
                tocTitles = new String[tocCount];
                int tocPos = pos;
                for (int i = 0; i < tocCount; i++) {
                    if (tocPos + 4 > pos + tocLen) break;
                    tocOffsets[i] = readInt32(data, tocPos);
                    tocPos += 4;
                }
                for (int i = 0; i < tocCount; i++) {
                    if (tocPos >= pos + tocLen) break;
                    int strLen = data[tocPos] & 0xFF;
                    tocPos++;
                    if (strLen > 0 && tocPos + strLen <= pos + tocLen) {
                        tocTitles[i] = readString(data, tocPos, strLen);
                        tocPos += strLen;
                    }
                }
            }
        }

        info.textSize = totalTextSize;

        return 0;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public int getTotalTextSize() {
        return totalTextSize;
    }

    public int getPageCount() {
        return pageOffsets != null ? pageOffsets.length : 0;
    }

    public int getTOCEntryCount() {
        return tocOffsets != null ? tocOffsets.length : 0;
    }

    public int getTOCEntryOffset(int index) {
        return tocOffsets != null && index >= 0 && index < tocOffsets.length
                ? tocOffsets[index] : -1;
    }

    public String getTOCEntryTitle(int index) {
        return tocTitles != null && index >= 0 && index < tocTitles.length
                ? tocTitles[index] : null;
    }

    private static int readInt16(byte[] data, int pos) {
        return ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
    }

    private static int readInt32(byte[] data, int pos) {
        return (data[pos] << 24) | ((data[pos + 1] & 0xFF) << 16)
                | ((data[pos + 2] & 0xFF) << 8) | (data[pos + 3] & 0xFF);
    }

    private static String readString(byte[] data, int pos, int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = (char) (data[pos + i] & 0xFF);
            if (c == 0) break;
            sb.append(c);
        }
        return sb.toString();
    }

    public static String readPalmString(byte[] data, int pos, int maxLen) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxLen; i++) {
            byte b = data[pos + i];
            if (b == 0) break;
            sb.append((char) (b & 0xFF));
        }
        return sb.toString();
    }
}
