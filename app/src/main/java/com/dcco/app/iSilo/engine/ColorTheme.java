package com.dcco.app.iSilo.engine;

import com.dcco.app.iSilo.engine.data.DataStream;
import com.dcco.app.iSilo.engine.util.ByteArrayUtils;
import com.dcco.app.iSilo.engine.util.ErrorUtil;
import com.dcco.app.iSilo.engine.util.StringCompare;

public final class ColorTheme {

    public int totalSize;
    public int themeCount;
    public int nextId;
    public int flags;
    public ThemeEntry[] entries;

    public static class Color {
        public byte byIdx;
        public byte byR;
        public byte byG;
        public byte byB;

        public Color() {
        }

        public Color(int idx, int rgb) {
            this.byIdx = (byte) idx;
            this.byR = (byte) (rgb >> 16);
            this.byG = (byte) (rgb >> 8);
            this.byB = (byte) rgb;
        }

        public static int getSize() {
            return 4;
        }

        public int readFrom(byte[] buf, int offset, int maxLen) {
            if (maxLen < 4) return 0;
            this.byIdx = buf[offset];
            this.byR = buf[offset + 1];
            this.byG = buf[offset + 2];
            this.byB = buf[offset + 3];
            return 4;
        }

        public int writeTo(byte[] buf, int offset, int maxLen) {
            if (maxLen < 4) return 0;
            buf[offset] = this.byIdx;
            buf[offset + 1] = this.byR;
            buf[offset + 2] = this.byG;
            buf[offset + 3] = this.byB;
            return 4;
        }
    }

    public ColorTheme() {
    }

    public int remove(int index) {
        if (index < 0 || index >= this.themeCount) return -2027749349;
        int sz = this.entries[index].getSize();
        if (index < this.themeCount - 1) {
            while (index < this.themeCount - 1) {
                this.entries[index] = this.entries[index + 1];
                index++;
            }
        }
        this.themeCount--;
        this.totalSize -= sz;
        return 0;
    }

    public int readFrom(DataStream stream) {
        try {
            byte[] header = new byte[8];
            if (ErrorUtil.isError(stream.readAt(0, header, 0, 8))) return 0;
            this.totalSize = ByteArrayUtils.readInt16BE(header, 0);
            this.themeCount = ByteArrayUtils.readInt16BE(header, 2);
            this.nextId = ByteArrayUtils.readInt16BE(header, 4);
            this.flags = ByteArrayUtils.readInt16BE(header, 6);
            try {
                byte[] data = new byte[this.totalSize];
                if (ErrorUtil.isError(stream.Read(data, 0, this.totalSize, null))) return 0;
                try {
                    ThemeEntry[] arr = new ThemeEntry[this.themeCount];
                    for (int i = 0; i < this.themeCount; i++) {
                        arr[i] = new ThemeEntry();
                    }
                    int pos = 0;
                    for (int i = 0; i < this.themeCount; i++) {
                        int read = arr[i].readFrom(data, pos, this.totalSize - pos);
                        if (read == 0) return 0;
                        pos += read;
                    }
                    this.entries = arr;
                    return this.totalSize + 8;
                } catch (OutOfMemoryError e) {
                    return 0;
                }
            } catch (OutOfMemoryError e) {
                return 0;
            }
        } catch (OutOfMemoryError e) {
            return 0;
        }
    }

    public int findByName(String name, int[] outIndex) {
        int lo = 0;
        int hi = this.themeCount - 1;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            int cmp = StringCompare.compareCaseInsensitive(name, this.entries[mid].name);
            if (cmp == 2) {
                hi = mid - 1;
            } else if (cmp == 3) {
                lo = mid + 1;
            } else {
                if (outIndex != null) outIndex[0] = mid;
                return 0;
            }
        }
        if (outIndex != null) outIndex[0] = -(lo + 1);
        return 1;
    }

    public int add(String name, Color[] colors, int[] outIndex) {
        if (this.themeCount >= 4096 || name.length() > 31) return -2027749349;
        try {
            int[] idxOut = new int[1];
            ThemeEntry entry = new ThemeEntry();
            int res = findByName(name, idxOut);
            if (ErrorUtil.isError(res)) return res;
            int idx = idxOut[0];
            if (idx >= 0) return -2027749349;
            int insertAt = (-idx) - 1;
            int attempt = 0;
            while (true) {
                if ((attempt & 0x8000) != 0) return -2027749349;
                if (this.nextId == 0 || (this.nextId & 0x8000) != 0) this.nextId = 1;
                int i = 0;
                while (i < this.themeCount && this.entries[i].id != this.nextId) i++;
                int newId = this.nextId;
                this.nextId++;
                if (i == this.themeCount) {
                    entry.id = newId;
                    entry.colorCount = colors.length;
                    entry.name = name;
                    entry.colors = colors;
                    int entrySize = entry.getSize();
                    if (this.totalSize + entrySize > 32000) return -2027749349;
                    try {
                        ThemeEntry[] newEntries = new ThemeEntry[this.themeCount + 1];
                        if (this.entries != null) {
                            int j = 0;
                            while (j < insertAt) {
                                newEntries[j] = this.entries[j];
                                j++;
                            }
                            while (true) {
                                j++;
                                if (j > this.themeCount) break;
                                newEntries[j] = this.entries[j - 1];
                            }
                        }
                        newEntries[insertAt] = entry;
                        this.entries = newEntries;
                        this.themeCount++;
                        this.totalSize += entrySize;
                        outIndex[0] = insertAt;
                        return 0;
                    } catch (OutOfMemoryError e) {
                        return -2147483646;
                    }
                }
                attempt++;
            }
        } catch (OutOfMemoryError e) {
            return -2147483646;
        }
    }

    public int writeTo(DataStream stream) {
        int size = 0;
        for (int i = 0; i < this.themeCount; i++) {
            size += this.entries[i].getSize();
        }
        this.totalSize = size;
        try {
            int total = this.totalSize + 8;
            byte[] buf = new byte[total];
            ByteArrayUtils.writeInt16BE(this.totalSize, buf, 0);
            ByteArrayUtils.writeInt16BE(this.themeCount, buf, 2);
            ByteArrayUtils.writeInt16BE(this.nextId, buf, 4);
            ByteArrayUtils.writeInt16BE(this.flags, buf, 6);
            int pos = 8;
            for (int i = 0; i < this.themeCount; i++) {
                ThemeEntry entry = this.entries[i];
                int remaining = total - pos;
                int nameLen = entry.name.length();
                if (remaining >= 4 && nameLen <= 31) {
                    ByteArrayUtils.writeInt16BE(entry.id, buf, pos);
                    buf[pos + 2] = (byte) entry.colorCount;
                    buf[pos + 3] = (byte) nameLen;
                    int p = pos + 4;
                    int avail = remaining - 4;
                    if (avail >= nameLen * 2) {
                        int written = 4;
                        for (int j = 0; j < nameLen; j++) {
                            ByteArrayUtils.writeInt16BE(entry.name.charAt(j), buf, p);
                            p += 2;
                        }
                        int remaining2 = avail - (nameLen * 2);
                        written += nameLen * 2;
                        for (int j = 0; j < entry.colorCount; j++) {
                            int cb = entry.colors[j].writeTo(buf, p, remaining2);
                            if (cb == 0) { written = 0; break; }
                            p += cb;
                            remaining2 -= cb;
                            written += cb;
                        }
                        if (written != 0) {
                            pos += written;
                            continue;
                        }
                    }
                }
                return 0;
            }
            if (ErrorUtil.isError(stream.writeAt(0, buf, 0, total))) return 0;
            return total;
        } catch (OutOfMemoryError e) {
            return 0;
        }
    }
}
