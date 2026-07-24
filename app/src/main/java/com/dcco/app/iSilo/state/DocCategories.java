package com.dcco.app.iSilo.state;

public final class DocCategories {
    public String name;
    public Object dataStream;
    public Object palmDB;
    public boolean isDirty;

    public DocCategories() {
    }

    public void reset() {
        this.name = null;
        this.dataStream = null;
        this.palmDB = null;
        this.isDirty = false;
    }
}
