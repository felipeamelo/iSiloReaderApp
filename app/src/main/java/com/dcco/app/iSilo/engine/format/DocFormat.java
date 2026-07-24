package com.dcco.app.iSilo.engine.format;

public abstract class DocFormat {

    public static final int ERR_OUT_OF_MEMORY = -2147483646;
    public static final int ERR_UNSUPPORTED = -2147483643;
    public static final int ERR_FILE_CORRUPT = -2028929015;
    public static final int ERR_END_OF_TEXT = 5;
    public static final int ERR_NOT_FOUND = -2146959348;

    protected iSiloDocInfo info;
    protected DocHeader header;

    public DocFormat() {
        this.info = new iSiloDocInfo();
    }

    public abstract int open(byte[] data, int size);

    public abstract int getText(int offset, int length, byte[] buffer);

    public abstract int findString(String query, int startOffset, int[] matchOffset, int[] matchLength);

    public iSiloDocInfo getInfo() {
        return info;
    }

    public DocHeader getHeader() {
        return header;
    }

    public int close() {
        return 0;
    }
}
