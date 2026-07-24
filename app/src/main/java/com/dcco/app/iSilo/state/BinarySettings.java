package com.dcco.app.iSilo.state;

import com.dcco.app.iSilo.engine.data.DataStream;
import com.dcco.app.iSilo.engine.data.FileDataStream;
import com.dcco.app.iSilo.engine.data.SerialBase;
import com.dcco.app.iSilo.engine.util.ByteArrayUtils;
import com.dcco.app.iSilo.state.AppState;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public final class BinarySettings extends SerialBase {

    private static final int SETTINGS_SIZE = 768;

    public int headerSize;
    public int fieldVersion;
    public short optionFlags;
    public short field_c;
    public long field_d;
    public byte[] uuid = new byte[16];
    public int field_L;
    public short field_f;
    public byte field_g;
    public byte field_h;
    public short field_i;
    public byte field_j;
    public byte field_k;
    public byte[] field_l = new byte[5];
    public byte field_m;
    public short field_n;
    public short field_o;
    public short field_p;
    public short field_q;
    public byte field_r;
    public byte field_s;
    public String field_t;
    public byte field_u;
    public byte field_v;
    public KeyMapping field_w = new KeyMapping();
    public byte[] keyList = new byte[128];
    public KeyProfile[] keyProfiles = new KeyProfile[4];
    public byte field_z;
    public int field_A;
    public long field_C;
    public long field_D;
    public long field_E;
    public int field_F;

    public static class KeyMapping {
        public short id;
        public byte field_b;
        public byte field_c;
        public byte field_d;
        public byte field_e;
        public byte field_f;
        public byte field_g;
    }

    public static class KeyProfile {
        public String name;

        public KeyProfile() {
        }
    }

    public BinarySettings() {
        for (int i = 0; i < 4; i++) {
            keyProfiles[i] = new KeyProfile();
        }
        init(SETTINGS_SIZE, null);
    }

    public boolean readFromFile(FileInputStream stream) {
        if (stream == null) return false;
        try {
            byte[] buf = new byte[SETTINGS_SIZE];
            try {
                if (stream.read(buf) < SETTINGS_SIZE) return false;
                setBuffer(buf);
                this.headerSize = readInt32(0);
                this.fieldVersion = readInt32(4);
                this.optionFlags = (short) readInt16(8);
                this.field_c = (short) readInt16(10);
                this.field_d = readLong(12);
                copyFromBuffer(16, this.uuid, 0, 20);
                this.field_L = readInt32(36);
                this.field_f = (short) readInt16(40);
                this.field_g = (byte) readUnsignedByte(42);
                this.field_h = (byte) readUnsignedByte(43);
                this.field_i = (short) readInt16(44);
                this.field_j = (byte) readUnsignedByte(46);
                this.field_k = (byte) readUnsignedByte(47);
                copyFromBuffer(48, this.field_l, 0, 5);
                this.field_m = (byte) readUnsignedByte(53);
                this.field_n = (short) readInt16(54);
                this.field_o = (short) readInt16(56);
                this.field_p = (short) readInt16(58);
                this.field_q = (short) readInt16(60);
                this.field_r = (byte) readUnsignedByte(62);
                this.field_s = (byte) readUnsignedByte(63);
                this.field_t = readString(64);
                this.field_u = (byte) readUnsignedByte(128);
                this.field_v = (byte) readUnsignedByte(129);
                this.field_w.id = (short) readInt16(130);
                this.field_w.field_b = (byte) readUnsignedByte(132);
                this.field_w.field_c = (byte) readUnsignedByte(133);
                this.field_w.field_d = (byte) readUnsignedByte(134);
                this.field_w.field_e = (byte) readUnsignedByte(135);
                this.field_w.field_f = (byte) readUnsignedByte(136);
                this.field_w.field_g = (byte) readUnsignedByte(137);
                copyFromBuffer(138, this.keyList, 0, 128);
                int pos = 266;
                for (int i = 0; i < 4; i++) {
                    this.keyProfiles[i].name = readString(pos);
                    pos += 64;
                }
                this.field_z = (byte) readUnsignedByte(522);
                this.field_A = readInt32(524);
                this.field_C = readLong(656);
                this.field_D = readLong(664);
                this.field_E = readLong(672);
                this.field_F = readInt32(680);
                System.arraycopy(this.buffer, 528, new byte[128], 0, 128);
                return true;
            } catch (Throwable t) {
                return false;
            }
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean writeToStream(FileOutputStream stream) {
        if (stream == null) return false;
        realloc();
        writeInt32(this.headerSize, 0);
        writeInt32(this.fieldVersion, 4);
        writeInt16(this.optionFlags, 8);
        writeInt16(this.field_c, 10);
        writeLong(this.field_d, 12);
        writeBytes(this.uuid, 20, 16);
        writeInt32(this.field_L, 36);
        writeInt16(this.field_f, 40);
        writeByte(this.field_g, 42);
        writeByte(this.field_h, 43);
        writeInt16(this.field_i, 44);
        writeByte(this.field_j, 46);
        writeByte(this.field_k, 47);
        writeBytes(this.field_l, 5, 48);
        writeByte(this.field_m, 53);
        writeInt16(this.field_n, 54);
        writeInt16(this.field_o, 56);
        writeInt16(this.field_p, 58);
        writeInt16(this.field_q, 60);
        writeByte(this.field_r, 62);
        writeByte(this.field_s, 63);
        writeString(this.field_t, 64);
        writeByte(this.field_u, 128);
        writeByte(this.field_v, 129);
        writeInt16(this.field_w.id, 130);
        writeByte(this.field_w.field_b, 132);
        writeByte(this.field_w.field_c, 133);
        writeByte(this.field_w.field_d, 134);
        writeByte(this.field_w.field_e, 135);
        writeByte(this.field_w.field_f, 136);
        writeByte(this.field_w.field_g, 137);
        writeBytes(this.keyList, 128, 138);
        int pos = 266;
        for (int i = 0; i < 4; i++) {
            writeString(this.keyProfiles[i].name, pos);
            pos += 64;
        }
        writeByte(this.field_z, 522);
        writeInt32(AppState.field_h, 524);
        System.arraycopy(new byte[128], 0, this.buffer, 528, 128);
        writeLong(this.field_C, 656);
        writeLong(this.field_D, 664);
        writeLong(this.field_E, 672);
        writeInt32(this.field_F, 680);
        try {
            stream.write(this.buffer);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public boolean save() {
        FileOutputStream stream;
        boolean ok = false;
        try {
            stream = AppState.appContext.openFileOutput("prefs", 0);
        } catch (FileNotFoundException e) {
            stream = null;
        }
        if (stream != null) {
            ok = writeToStream(stream);
            try {
                stream.close();
            } catch (IOException e) {
            }
        }
        return ok;
    }

    public static int readInt24(DataStream stream, int offset) {
        byte[] buf = new byte[4];
        if (stream.readAt(offset, buf, 0, 4) < 0) return -1;
        return ByteArrayUtils.readInt32BE(buf, 0);
    }
}
