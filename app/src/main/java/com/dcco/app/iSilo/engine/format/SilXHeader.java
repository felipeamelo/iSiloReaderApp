package com.dcco.app.iSilo.engine.format;

import com.dcco.app.iSilo.engine.util.AppLog;

public class SilXHeader {

    private byte[] data;
    private int offset;

    public int headerSize;
    public int versionMajor;
    public int versionMinor;
    public int versionPatch;
    public boolean validMagic;

    public int layoutInfo;
    public int columnWidth1;
    public int columnWidth2;
    public int columnWidth3;
    public int columnWidth4;
    public int columnWidth5;
    public int columnWidth6;

    public int totalTextSize;
    public int blockUnitSize;
    public int pageCount;
    public int titleFontSize;
    public int bodyFontSize;
    public int docFlags;
    public int encodingFlags;
    public int crc;
    public int fontAttr1;
    public int fontAttr2;
    public int lastPageIndex;
    public int embeddingType;

    public int groupCount;
    public int[] A = new int[4];
    public int[] recordStarts = new int[18];
    public int[] recordCounts = new int[18];

    public SilXHeader() {
    }

    public int parse(byte[] record0, int off, int len) {
        this.data = record0;
        this.offset = off;

        if (len < 128) return -2028929015;

        headerSize = f(0);
        versionMajor = d(2);
        versionMinor = d(3);
        versionPatch = d(4);

        validMagic = (d(6) == 'i' && d(7) == 's'
                && d(8) == 'S' && d(9) == 'i'
                && d(10) == 'l' && d(11) == 'o');

        AppLog.add("SILX_HEADER", "f(0)=%d d(2)=%d d(3)=%d d(4)=%d",
                headerSize, versionMajor, versionMinor, versionPatch);
        AppLog.add("SILX_HEADER", "magic=isSilo valid=%s", validMagic);
        AppLog.hex("SILX_RAW0", data, offset, 64);

        if (!validMagic) return -2028929015;

        layoutInfo = f(12);
        columnWidth1 = d(14);
        columnWidth2 = d(15);
        columnWidth3 = d(16);
        columnWidth4 = d(17);
        columnWidth5 = d(18);
        columnWidth6 = d(19);

        totalTextSize = g(20);
        blockUnitSize = f(24);
        pageCount = f(30);
        titleFontSize = f(32);
        bodyFontSize = f(34);
        docFlags = f(36);
        encodingFlags = f(38);
        crc = g(40);
        fontAttr1 = d(52);
        fontAttr2 = d(53);
        lastPageIndex = f(56);
        embeddingType = f(58);

        AppLog.add("SILX_HEADER", "g(20)=totalTextSize=%d", totalTextSize);
        AppLog.add("SILX_HEADER", "f(24)=blockUnitSize=%d", blockUnitSize);
        AppLog.add("SILX_HEADER", "f(30)=pageCount=%d", pageCount);
        AppLog.add("SILX_HEADER", "f(32)=titleFontSz=%d f(34)=bodyFontSz=%d", titleFontSize, bodyFontSize);
        AppLog.add("SILX_HEADER", "f(36)=docFlags=0x%04x f(38)=encodingFlags=0x%04x", docFlags, encodingFlags);
        AppLog.add("SILX_HEADER", "g(40)=crc=0x%08x", crc);
        AppLog.add("SILX_HEADER", "d(52)=fontAttr1=0x%02x d(53)=fontAttr2=0x%02x", fontAttr1, fontAttr2);
        AppLog.add("SILX_HEADER", "f(56)=lastPageIdx=%d f(58)=embeddingType=%d", lastPageIndex, embeddingType);
        AppLog.add("SILX_HEADER", "g(48)highNibble=0x%08x", g(48));

        if (len < headerSize + 4) {
            AppLog.add("SILX_HEADER", "too short for group table");
            return 0;
        }

        int pos = headerSize;
        groupCount = f(pos);
        int maxGroups = groupCount > 4 ? 4 : groupCount;
        pos += 2;
        AppLog.add("SILX_GROUPS", "count=%d", groupCount);
        for (int i = 0; i < maxGroups; i++) {
            A[i] = f(pos);
            pos += 2;
            AppLog.add("SILX_GROUPS", "  A[%d]=%d", i, A[i]);
        }
        if (groupCount > 4) {
            pos += (groupCount - 4) * 2;
        }

        int kCount = f(pos);
        pos += 2;
        int maxK = kCount > 18 ? 18 : kCount;
        AppLog.add("SILX_GROUPS", "kCount=%d", kCount);
        for (int i = 0; i < maxK; i++) {
            recordStarts[i] = f(pos);
            pos += 2;
        }
        for (int i = 0; i < maxK; i++) {
            recordCounts[i] = f(pos);
            pos += 2;
        }
        AppLog.add("SILX_GROUPS", "  k[0]=%d B[0]=%d", recordStarts[0], recordCounts[0]);

        return 0;
    }

    public void fillInfo(iSiloDocInfo info) {
        info.formatVersion = versionMajor;
        info.docVersion = (versionMajor << 8) | versionMinor;
        info.pageCount = pageCount;
        info.rawEncodingFlags = encodingFlags;
        info.charset = (encodingFlags == 106) ? iSiloDocInfo.CHARSET_UTF8 : iSiloDocInfo.CHARSET_LATIN1;
        info.compressed = (docFlags & 1) == 0;
        info.encrypted = (docFlags & 2) != 0;
        info.hasTOC = (docFlags & 4) != 0;
        info.hasImages = (docFlags & 8) != 0;
        info.compressionType = iSiloDocInfo.COMPRESSION_HUFFMAN;
        System.arraycopy(A, 0, info.groupA, 0, 4);
        System.arraycopy(recordStarts, 0, info.recordStarts, 0, 18);
        System.arraycopy(recordCounts, 0, info.recordCounts, 0, 18);
    }

    private int d(int i) {
        return data[offset + i] & 0xFF;
    }

    private int f(int i) {
        return ((data[offset + i] & 0xFF) << 8) | (data[offset + i + 1] & 0xFF);
    }

    private int g(int i) {
        return (data[offset + i] << 24)
                | ((data[offset + i + 1] & 0xFF) << 16)
                | ((data[offset + i + 2] & 0xFF) << 8)
                | (data[offset + i + 3] & 0xFF);
    }
}
