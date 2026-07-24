package com.dcco.app.iSilo.engine.format;

public class LinkEntry {
    public int charOffset;
    public int targetOffset;
    public int length;
    public String title;

    public LinkEntry(int charOffset, int targetOffset, int length, String title) {
        this.charOffset = charOffset;
        this.targetOffset = targetOffset;
        this.length = length;
        this.title = title;
    }
}
