package com.dcco.app.iSilo.engine.render;

import com.dcco.app.iSilo.engine.util.DebugLog;

public class StyleTableParser {

    private static final int FONT_COUNT = 13;

    public static class Result {
        public TextStyle[] fontTable;
        public byte[] styleData;
    }

    public static Result parse(byte[] recordData, int recordSize,
                                int silxHeaderSize, int groupA0,
                                int bodyFontSize, int fontAttr1, int fontAttr2) {
        Result result = new Result();
        int baseSize = clampFontSize(bodyFontSize);
        result.fontTable = buildDefaultFontTable(baseSize, fontAttr1, fontAttr2);
        result.styleData = new byte[0];

        return result;
    }

    private static int clampFontSize(int raw) {
        if (raw >= 8 && raw <= 32) return raw;
        if (raw > 32 && raw < 128) return raw / 2;
        if (raw >= 128) return 20;
        return 20;
    }

    private static int mapTypeface(int styleId) {
        switch (styleId) {
            case 0: case 1: case 2: case 3: return TextStyle.FACESERIF;
            case 4: case 5: case 6: case 7: return TextStyle.FACESANS;
            case 12: return TextStyle.FACEMONO;
            default: return TextStyle.FACEDEFAULT;
        }
    }

    private static TextStyle[] buildDefaultFontTable(int baseSize, int fontAttr1, int fontAttr2) {
        TextStyle[] table = new TextStyle[FONT_COUNT];
        if (baseSize < 8) baseSize = 20;

        int face1 = (fontAttr1 & 0xF0) >> 4;
        int face2 = fontAttr1 & 0x0F;
        if (face1 == 0 || face1 > 5) face1 = TextStyle.FACESERIF;
        if (face2 == 0 || face2 > 5) face2 = TextStyle.FACESANS;

        for (int i = 0; i < FONT_COUNT; i++) {
            table[i] = new TextStyle();
        }

        table[0].fontSize = baseSize;
        table[0].typeface = face1;

        table[1].fontSize = baseSize;
        table[1].typeface = face1;
        table[1].bold = true;

        table[2].fontSize = baseSize;
        table[2].typeface = face1;
        table[2].italic = true;

        table[3].fontSize = baseSize;
        table[3].typeface = face1;
        table[3].bold = true;
        table[3].italic = true;

        table[4].fontSize = Math.max(Math.round(baseSize * 0.7f), 10);
        table[4].typeface = face2;

        table[5].fontSize = Math.max(Math.round(baseSize * 0.7f), 10);
        table[5].typeface = face2;
        table[5].bold = true;

        table[6].fontSize = Math.max(Math.round(baseSize * 0.7f), 10);
        table[6].typeface = face2;
        table[6].italic = true;

        table[7].fontSize = Math.max(Math.round(baseSize * 0.7f), 10);
        table[7].typeface = face2;
        table[7].bold = true;
        table[7].italic = true;

        table[8].fontSize = Math.min(Math.round(baseSize * 1.4f), 36);
        table[8].typeface = face1;
        table[9].fontSize = Math.min(Math.round(baseSize * 1.4f), 36);
        table[9].typeface = face1;
        table[9].bold = true;

        table[10].fontSize = Math.min(Math.round(baseSize * 1.2f), 32);
        table[10].typeface = face1;
        table[11].fontSize = Math.min(Math.round(baseSize * 1.2f), 32);
        table[11].typeface = face1;
        table[11].bold = true;

        table[12].fontSize = baseSize;
        table[12].typeface = TextStyle.FACEMONO;

        return table;
    }

    private static int read16(byte[] data, int off) {
        return ((data[off] & 0xFF) << 8) | (data[off + 1] & 0xFF);
    }
}
