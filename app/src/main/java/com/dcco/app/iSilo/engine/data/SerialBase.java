package com.dcco.app.iSilo.engine.data;

public abstract class SerialBase {
    public byte[] buffer;
    public int dataOffset;
    public int dataSize;
    public int maxSize;

    public SerialBase() {
    }

    public final int readFrom(DataStream stream) {
        return stream.readAt(0, this.buffer, 0, this.dataSize);
    }

    public final void init(int size, byte[] buf) {
        this.dataSize = size;
        this.buffer = buf;
        if (buf == null) {
            this.buffer = new byte[size];
        }
        this.dataOffset = 0;
        this.maxSize = this.dataSize;
    }

    public final void copyFromBuffer(int srcOffset, byte[] dst, int dstOffset, int length) {
        System.arraycopy(this.buffer, this.dataOffset + srcOffset, dst, 0, length);
    }

    public final void writeByte(int value, int offset) {
        this.buffer[this.dataOffset + offset] = (byte) value;
    }

    public final void writeInt16(int value, int offset) {
        int pos = this.dataOffset + offset;
        this.buffer[pos] = (byte) (value >> 8);
        this.buffer[pos + 1] = (byte) value;
    }

    public final void writeInt32(int value, int offset) {
        int pos = this.dataOffset + offset;
        this.buffer[pos] = (byte) (value >> 24);
        this.buffer[pos + 1] = (byte) (value >> 16);
        this.buffer[pos + 2] = (byte) (value >> 8);
        this.buffer[pos + 3] = (byte) value;
    }

    public final void writeLong(long value, int offset) {
        int pos = this.dataOffset + offset + 8;
        for (int i = 7; i >= 0; i--) {
            pos--;
            this.buffer[pos] = (byte) value;
            value >>= 8;
        }
    }

    public final void writeString(String str, int offset) {
        int pos = this.dataOffset + offset;
        int len = str == null ? 0 : str.length();
        int i = 0;
        while (i < len) {
            char c = str.charAt(i);
            this.buffer[pos++] = (byte) (c >> 8);
            this.buffer[pos++] = (byte) c;
            i++;
        }
        while (i <= 31) {
            this.buffer[pos++] = 0;
            this.buffer[pos++] = 0;
            i++;
        }
    }

    public final void writeBytes(byte[] src, int srcLen, int dstOffset) {
        System.arraycopy(src, 0, this.buffer, this.dataOffset + dstOffset, srcLen);
    }

    public final void copyFrom(SerialBase other) {
        int len = this.dataSize;
        if (len > other.dataSize) len = other.dataSize;
        for (int i = len - 1; i >= 0; i--) {
            this.buffer[i] = other.buffer[i];
        }
    }

    public final void copyTo(SerialBase other) {
        this.buffer = other.buffer;
        this.dataOffset = other.dataOffset;
        this.dataSize = other.dataSize;
        this.maxSize = other.maxSize;
    }

    public final void setBuffer(byte[] buf, int offset) {
        this.buffer = buf;
        this.dataOffset = offset;
        this.dataSize = this.maxSize;
    }

    public final void setBuffer(byte[] buf) {
        this.buffer = buf;
        this.dataOffset = 0;
        this.dataSize = this.maxSize;
    }

    public final void realloc() {
        init(this.dataSize, null);
    }

    public final void free() {
        this.buffer = null;
        this.dataOffset = 0;
        this.dataSize = 0;
    }

    public final int readUnsignedByte(int offset) {
        return this.buffer[this.dataOffset + offset] & 0xFF;
    }

    public final int readSignedByte(int offset) {
        return this.buffer[this.dataOffset + offset];
    }

    public final int readInt16(int offset) {
        int pos = this.dataOffset + offset;
        return (this.buffer[pos + 1] & 0xFF) | ((this.buffer[pos] << 8) & 0xFF00);
    }

    public final int readInt32(int offset) {
        int pos = this.dataOffset + offset;
        return (this.buffer[pos + 3] & 0xFF) | (this.buffer[pos] << 24)
                | ((this.buffer[pos + 1] << 16) & 0xFF0000) | ((this.buffer[pos + 2] << 8) & 0xFF00);
    }

    public final long readLong(int offset) {
        int pos = this.dataOffset + offset;
        long val = this.buffer[pos] & 0xFF;
        for (int i = 1; i < 8; i++) {
            val = (val << 8) | (long) (this.buffer[pos + i] & 0xFF);
        }
        return val;
    }

    public final String readString(int offset) {
        StringBuilder sb = new StringBuilder(31);
        int pos = this.dataOffset + offset;
        int end = pos + 62;
        while (pos < end) {
            char c = (char) (((this.buffer[pos] & 0xFF) << 8) | (this.buffer[pos + 1] & 0xFF));
            if (c == 0) break;
            sb.append(c);
            pos += 2;
        }
        return sb.toString();
    }

    public final void clear() {
        if (this.buffer == null) return;
        for (int i = 0; i < this.dataSize; i++) {
            this.buffer[i] = 0;
        }
    }
}
