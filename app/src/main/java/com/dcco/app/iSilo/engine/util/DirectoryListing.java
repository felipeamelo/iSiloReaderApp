package com.dcco.app.iSilo.engine.util;

import java.util.Enumeration;

public class DirectoryListing implements Enumeration<String> {
    private final String[] entries;
    private int index;

    public DirectoryListing(String[] entries) {
        this.entries = entries;
        this.index = 0;
    }

    @Override
    public boolean hasMoreElements() {
        return index < entries.length;
    }

    @Override
    public String nextElement() {
        return entries[index++];
    }
}
