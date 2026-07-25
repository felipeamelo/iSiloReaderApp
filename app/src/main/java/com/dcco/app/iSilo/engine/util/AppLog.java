package com.dcco.app.iSilo.engine.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppLog {

    private static final StringBuilder log = new StringBuilder(16384);

    static {
        log.append("=== iSiloReader v1.1.1 [" + new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()) + "] ===\n");
    }

    public static void clear() {
        synchronized (log) {
            log.setLength(0);
            log.append("=== iSiloReader v1.1.1 [" + new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()) + "] ===\n");
        }
    }

    public static void add(String tag, String msg) {
        synchronized (log) {
            log.append(tag).append(": ").append(msg).append("\n");
        }
    }

    public static void add(String tag, String format, Object... args) {
        synchronized (log) {
            log.append(tag).append(": ").append(String.format(format, args)).append("\n");
        }
    }

    public static void hex(String tag, byte[] data, int off, int len) {
        if (data == null) { add(tag, "null"); return; }
        synchronized (log) {
            StringBuilder sb = new StringBuilder();
            int end = Math.min(off + len, data.length);
            for (int i = off; i < end; i++) {
                sb.append(String.format("%02X ", data[i]));
            }
            log.append(tag).append(": ").append(sb.toString().trim()).append("\n");
        }
    }

    public static String get() {
        synchronized (log) {
            return log.toString();
        }
    }
}
