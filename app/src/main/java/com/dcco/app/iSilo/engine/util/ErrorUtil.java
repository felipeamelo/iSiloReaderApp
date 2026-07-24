package com.dcco.app.iSilo.engine.util;

public class ErrorUtil {

    public static final int ERR_OOM = -2147483646;
    public static final int ERR_UNSUPPORTED = -2147483643;
    public static final int ERR_NOT_FOUND = -2146959348;
    public static final int ERR_GENERIC = Integer.MIN_VALUE;

    public static boolean isError(int code) {
        return code < 0;
    }

    public static boolean isSuccess(int code) {
        return code >= 0;
    }
}
