package com.dcco.app.iSilo.state;

import android.content.Context;

public final class AppState {
    public static boolean isARM;
    public static boolean field_b;
    public static boolean field_c;
    public static int sdkVersion;
    public static Object engine;
    public static String field_f;
    public static int field_g;
    public static int field_h;
    public static Context appContext;
    public static Context activityContext;
    public static boolean field_k;
    public static boolean field_l;
    public static int field_m;
    public static int field_n;
    public static int field_o;
    public static Object docNavigator;
    public static Object activityGroup;
    public static Object docListActivity;
    public static Object readActivity;
    public static SessionState sessionState;
    public static DocState docState;
    public static LoadState loadState;
    public static StringSettings stringSettings = new StringSettings();
    public static BinarySettings binarySettings = new BinarySettings();
    public static int[] cryptoState = new int[16];
    public static Object cryptoManager;
}
