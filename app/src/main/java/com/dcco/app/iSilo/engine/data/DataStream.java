package com.dcco.app.iSilo.engine.data;

import com.dcco.app.iSilo.engine.util.ErrorUtil;

public abstract class DataStream {

    public DataStream() {
    }

    public int Close() {
        return -2147483643;
    }

    public int GetSize(int[] sizeOut) {
        return -2147483643;
    }

    public int PreAllocSpace(int size) {
        return -2147483643;
    }

    public int Read(byte[] buf, int offset, int length, int[] bytesRead) {
        return -2147483643;
    }

    public int Seek(int pos, int mode, int[] resultOut) {
        return -2147483643;
    }

    public int SetAllocationSize(int size) {
        return -2147483643;
    }

    public int SetSize(int size) {
        return -2147483643;
    }

    public int Write(byte[] buf, int offset, int length, int[] bytesWritten) {
        return -2147483643;
    }

    public final int readAt(int offset, byte[] buf, int bufOffset, int length) {
        int res = Seek(offset, 0, null);
        if (ErrorUtil.isError(res)) return res;
        return Read(buf, bufOffset, length, null);
    }

    public final int writeAt(int offset, byte[] buf, int bufOffset, int length) {
        int res = Seek(offset, 0, null);
        if (ErrorUtil.isError(res)) return res;
        return Write(buf, bufOffset, length, null);
    }

    public final int writeString(int offset, String str) {
        int res = Seek(offset, 0, null);
        if (ErrorUtil.isError(res)) return res;
        return writeStringInternal(str);
    }

    public final int writeInt16At(int offset, int value) {
        int res = Seek(offset, 0, null);
        if (ErrorUtil.isError(res)) return res;
        return writeInt16(value);
    }

    public final int writeInt32At(int offset, int value) {
        int res = Seek(offset, 0, null);
        if (ErrorUtil.isError(res)) return res;
        return writeInt32(value);
    }

    private int writeStringInternal(String str) {
        int len = str.length();
        if (len == 0) return 0;
        try {
            byte[] buf = new byte[len * 2];
            int pos = 0;
            for (int i = 0; i < len; i++) {
                char c = str.charAt(i);
                buf[pos] = (byte) (c >> 8);
                buf[pos + 1] = (byte) c;
                pos += 2;
            }
            return Write(buf, 0, pos, null);
        } catch (OutOfMemoryError e) {
            return -2147483646;
        }
    }

    private int writeInt16(int value) {
        try {
            byte[] buf = new byte[2];
            buf[0] = (byte) (value >> 8);
            buf[1] = (byte) value;
            int res = Write(buf, 0, 2, null);
            if (ErrorUtil.isError(res)) return res;
            return 0;
        } catch (OutOfMemoryError e) {
            return -2147483646;
        }
    }

    private int writeInt32(int value) {
        try {
            byte[] buf = new byte[4];
            buf[0] = (byte) (value >> 24);
            buf[1] = (byte) (value >> 16);
            buf[2] = (byte) (value >> 8);
            buf[3] = (byte) value;
            int res = Write(buf, 0, 4, null);
            if (ErrorUtil.isError(res)) return res;
            return 0;
        } catch (OutOfMemoryError e) {
            return -2147483646;
        }
    }

    public final int writeString(String str, int offset) {
        int res = Seek(offset, 0, null);
        if (ErrorUtil.isError(res)) return res;
        return writeStringInternal(str);
    }

    public final int writeSerialBase(SerialBase sb) {
        return writeAt(0, sb.buffer, 0, sb.dataSize);
    }

    public final int readSerialBase(SerialBase sb) {
        return readAt(0, sb.buffer, 0, sb.dataSize);
    }

    public final int writeInt32Array(int[] values, int count) {
        int byteLen = count << 2;
        try {
            byte[] buf = new byte[byteLen];
            int pos = byteLen;
            for (int i = count - 1; i >= 0; i--) {
                int v = values[i];
                buf[--pos] = (byte) v;
                buf[--pos] = (byte) (v >> 8);
                buf[--pos] = (byte) (v >> 16);
                buf[--pos] = (byte) (v >> 24);
            }
            int res = Write(buf, 0, byteLen, null);
            if (ErrorUtil.isError(res)) return res;
            return 0;
        } catch (OutOfMemoryError e) {
            return -2147483646;
        }
    }

    public final int readInt16Array(short[] values, int count) {
        int byteLen = count << 1;
        try {
            byte[] buf = new byte[byteLen];
            int res = Read(buf, 0, byteLen, null);
            if (ErrorUtil.isError(res)) return res;
            int pos = byteLen;
            for (int i = count - 1; i >= 0; i--) {
                pos -= 2;
                values[i + 1] = (short) (((buf[pos] & 0xFF) << 8) | (buf[pos + 1] & 0xFF));
            }
            return 0;
        } catch (OutOfMemoryError e) {
            return -2147483646;
        }
    }

    public final int writeInt16Array(short[] values, int count) {
        int byteLen = count << 1;
        try {
            byte[] buf = new byte[byteLen];
            int pos = byteLen;
            for (int i = count - 1; i >= 0; i--) {
                short s = values[i];
                buf[--pos] = (byte) s;
                buf[--pos] = (byte) (s >> 8);
            }
            int res = Write(buf, 0, byteLen, null);
            if (ErrorUtil.isError(res)) return res;
            return 0;
        } catch (OutOfMemoryError e) {
            return -2147483646;
        }
    }

    public final int copyTo(DataStream dest, int srcOffset, int destOffset, int length) {
        if (length == 0) return 0;
        try {
            int[] sizeOut = new int[1];
            if (ErrorUtil.isError(GetSize(sizeOut))) return -2147024895;
            if (srcOffset > sizeOut[0]) return -2147024894;
            if (sizeOut[0] - srcOffset < length) length = sizeOut[0] - srcOffset;

            int blockSize = 16384;
            while (true) {
                if (length >= blockSize) {
                    try {
                        byte[] buf = new byte[blockSize];
                        if (this != dest || srcOffset >= destOffset || destOffset >= srcOffset + length) {
                            int bs = blockSize;
                            for (int remaining = length; remaining != 0; remaining -= bs) {
                                if (remaining < bs) bs = remaining;
                                if (ErrorUtil.isError(Seek(srcOffset, 0, null))) return -2147024893;
                                if (ErrorUtil.isError(Read(buf, 0, bs, null))) return -2147024891;
                                if (ErrorUtil.isError(dest.Seek(destOffset, 0, null))) return -2147024892;
                                if (ErrorUtil.isError(dest.Write(buf, 0, bs, null))) return -2147024890;
                                srcOffset += bs;
                                destOffset += bs;
                            }
                        } else {
                            if (ErrorUtil.isError(GetSize(sizeOut))) return -2147024889;
                            if (sizeOut[0] < destOffset + length && ErrorUtil.isError(SetSize(destOffset + length)))
                                return -2147024888;
                            int srcEnd = srcOffset + length;
                            int dstEnd = destOffset + length;
                            int bs = blockSize;
                            for (int remaining = length; remaining != 0; remaining -= bs) {
                                if (remaining < bs) bs = remaining;
                                srcEnd -= bs;
                                dstEnd -= bs;
                                if (ErrorUtil.isError(Seek(srcEnd, 0, null))) return -2147024893;
                                if (ErrorUtil.isError(Read(buf, 0, bs, null))) return -2147024891;
                                if (ErrorUtil.isError(Seek(dstEnd, 0, null))) return -2147024892;
                                if (ErrorUtil.isError(Write(buf, 0, bs, null))) return -2147024890;
                            }
                        }
                        return 0;
                    } catch (OutOfMemoryError e) {
                    }
                }
                blockSize >>= 1;
                if (blockSize == 0) return -2147483646;
            }
        } catch (OutOfMemoryError e) {
            return -2147483646;
        }
    }

    public final int zeroFill(int size) {
        int blockSize = 16384;
        do {
            if (2 >= blockSize) {
                try {
                    byte[] buf = new byte[blockSize];
                    int writeLen = 2 >= blockSize ? blockSize : 2;
                    java.util.Arrays.fill(buf, 0, writeLen, (byte) 0);
                    try {
                        int[] written = new int[1];
                        for (int remaining = size; remaining != 0; remaining -= writeLen) {
                            if (writeLen > remaining) writeLen = remaining;
                            int res = Write(buf, 0, writeLen, written);
                            if (ErrorUtil.isError(res)) return res;
                            if (written[0] != writeLen) return -2147024886;
                        }
                        return 0;
                    } catch (OutOfMemoryError e) {
                        return -2147483646;
                    }
                } catch (OutOfMemoryError e) {
                }
            }
            blockSize >>= 1;
        } while (blockSize != 0);
        return -2147483646;
    }
}
