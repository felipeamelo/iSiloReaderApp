package com.dcco.app.iSilo.state;

public final class LoadState {
    public static final int INITIAL = 0;
    public static final int LOADING = 3;
    public static final int READY = 102;
    public static final int ERROR = 100;

    public int state = INITIAL;
    public int field_b;
    public int field_c;
    public int field_d;
    public Object field_e;
    public int errorMessageResId;
    public String password;

    public LoadState() {
    }
}
