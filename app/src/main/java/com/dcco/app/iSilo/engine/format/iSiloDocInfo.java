package com.dcco.app.iSilo.engine.format;

public class iSiloDocInfo {
    public int formatVersion;
    public int docVersion;
    public String title;
    public String author;
    public String publisher;
    public String isbn;
    public String copyright;
    public String description;
    public int textSize;
    public int pageCount;
    public boolean hasTOC;
    public boolean hasImages;
    public boolean encrypted;
    public boolean compressed;
    public int compressionType;
    public int charset;
    public int rawEncodingFlags;
    public int[] groupA = new int[4];
    public int[] recordStarts = new int[18];
    public int[] recordCounts = new int[18];
    public int[] pageOffsets;
    public java.util.ArrayList<LinkEntry> links;
    public String[] tocTitles;
    public int[] tocOffsets;

    public static final int COMPRESSION_NONE = 0;
    public static final int COMPRESSION_LZ77 = 1;
    public static final int COMPRESSION_HUFFMAN = 2;
    public static final int COMPRESSION_BOTH = 3;

    public static final int CHARSET_ASCII = 0;
    public static final int CHARSET_LATIN1 = 1;
    public static final int CHARSET_UTF8 = 2;
    public static final int CHARSET_UTF16BE = 3;
    public static final int CHARSET_SHIFT_JIS = 4;
    public static final int CHARSET_BIG5 = 5;

    public iSiloDocInfo() {
        formatVersion = 3;
        compressed = true;
        compressionType = COMPRESSION_LZ77;
    }

    public int getFormatFlags() {
        int flags = 0;
        if (formatVersion >= 4) flags |= 1;
        if (hasImages) flags |= 2;
        if (hasTOC) flags |= 4;
        if (encrypted) flags |= 8;
        return flags;
    }
}
