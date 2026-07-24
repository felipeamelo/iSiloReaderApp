package com.dcco.app.iSilo.engine.util;

public abstract class iSiloInflator {

    public static final int ERR_UNSUPPORTED = -2147483643;

    public int GetTrees(byte[] buf, int offset, int length, int flags) {
        return ERR_UNSUPPORTED;
    }

    public int InflateBlock(byte[] inBuf, int inOff, int inLen,
                            byte[] outBuf, int outOff, int outLen,
                            int[] bytesUsed) {
        return ERR_UNSUPPORTED;
    }
}
