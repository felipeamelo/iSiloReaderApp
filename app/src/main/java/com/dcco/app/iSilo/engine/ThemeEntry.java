package com.dcco.app.iSilo.engine;

import com.dcco.app.iSilo.engine.ColorTheme.Color;

public class ThemeEntry {
    public int id;
    public int colorCount;
    public String name;
    public Color[] colors;

    public ThemeEntry() {
    }

    public int getSize() {
        int sz = 4;
        if (name != null) sz += name.length() * 2;
        if (colors != null) sz += colors.length * 4;
        return sz;
    }

    public int readFrom(byte[] buf, int offset, int maxLen) {
        if (maxLen < 4) return 0;
        id = ((buf[offset] & 0xFF) << 8) | (buf[offset + 1] & 0xFF);
        colorCount = buf[offset + 2] & 0xFF;
        int nameLen = buf[offset + 3] & 0xFF;
        int p = offset + 4;
        int remaining = maxLen - 4;
        if (remaining < nameLen * 2) return 0;
        StringBuilder sb = new StringBuilder(nameLen);
        for (int i = 0; i < nameLen; i++) {
            char c = (char) (((buf[p] & 0xFF) << 8) | (buf[p + 1] & 0xFF));
            sb.append(c);
            p += 2;
        }
        name = sb.toString();
        remaining -= nameLen * 2;
        colors = new Color[colorCount];
        for (int i = 0; i < colorCount; i++) {
            colors[i] = new Color();
            int cb = colors[i].readFrom(buf, p, remaining);
            if (cb == 0) return 0;
            p += cb;
            remaining -= cb;
        }
        return p - offset;
    }
}
