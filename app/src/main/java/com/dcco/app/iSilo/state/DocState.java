package com.dcco.app.iSilo.state;

public final class DocState {

    public Object palmDB;
    public Object dataStream;
    public Object document;
    public boolean field_e;
    public boolean field_f;
    public boolean field_g;
    public int flags;
    public int scrollY;
    public int field_j;
    public int field_k;
    public boolean field_l;
    public int pageNumber;
    public int field_o;
    public int field_p;
    public int docType;
    public String filePath;
    public DocCategories categories = new DocCategories();
    public byte[] field_n = new byte[18];

    public DocState() {
    }

    public void reset() {
        this.palmDB = null;
        this.dataStream = null;
        this.document = null;
        this.categories.reset();
        this.field_e = false;
        this.field_f = true;
        this.field_g = false;
        this.flags = 0;
        this.scrollY = AppState.binarySettings.field_n;
        this.field_j = AppState.binarySettings.field_o;
        this.field_k = AppState.binarySettings.field_p;
        this.field_l = false;
        this.pageNumber = 0;
        for (int i = 0; i < 18; i++) {
            this.field_n[i] = 0;
        }
        this.filePath = null;
    }

    public void destroy() {
        if (this.document != null) {
            destroyDocument();
            this.document = null;
        }
        if (AppState.docNavigator != null) {
            saveCategories();
        }
        if (this.palmDB != null) {
            destroyPalmDB();
            this.palmDB = null;
        }
        if (this.dataStream != null) {
            closeDataStream();
            this.dataStream = null;
        }
        reset();
    }

    private native void destroyDocument();
    private native void destroyPalmDB();
    private native void closeDataStream();
    private native void saveCategories();
}
