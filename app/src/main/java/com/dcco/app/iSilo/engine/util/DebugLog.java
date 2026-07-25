package com.dcco.app.iSilo.engine.util;

public class DebugLog {

    private static StringBuilder log = new StringBuilder(8192);

    static {
        log.append("=== iSiloReader v1.1.1 ===\n");
    }

    public static void clear() {
        log = new StringBuilder(8192);
        log.append("=== iSiloReader v1.1.1 ===\n");
    }

    public static void add(String tag, String msg) {
        log.append(tag).append(": ").append(msg).append("\n");
    }

    public static void add(String tag, String format, Object... args) {
        log.append(tag).append(": ").append(String.format(format, args)).append("\n");
    }

    public static void hex(String tag, byte[] data, int off, int len) {
        if (data == null) { add(tag, "null"); return; }
        StringBuilder sb = new StringBuilder();
        int end = Math.min(off + len, data.length);
        for (int i = off; i < end; i++) {
            sb.append(String.format("%02X ", data[i]));
        }
        log.append(tag).append(": ").append(sb.toString().trim()).append("\n");
    }

    public static void hex(String tag, byte[] data) {
        hex(tag, data, 0, data != null ? data.length : 0);
    }

    public static void addr(String tag, String msg, int value) {
        log.append(tag).append(": ").append(msg).append("=").append(value)
            .append(" (0x").append(String.format("%08X", value)).append(")\n");
    }

    public static String get() {
        return log.toString();
    }
}
