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

        if (recordData == null || recordSize < 8) return result;

        DebugLog.hex("STYLE_RECORD", recordData, 0, Math.min(recordSize, 64));

        int fxOff = recordData[0] & 0xFF;
        int subType = recordData[1] & 0xFF;

        DebugLog.add("STYLE_PARSE", "byte[0]=%d byte[1]=%d recordSize=%d", fxOff, subType, recordSize);

        if (fxOff < 4 || fxOff + 4 > recordSize) {
            DebugLog.add("STYLE_PARSE", "invalid fx offset=%d", fxOff);
            return result;
        }

        int entryCount = (recordData[fxOff + 2] & 0xFF);
        int strideBase = (recordData[fxOff + 3] & 0xFF);
        int fwOffIncr = (recordData[fxOff] & 0xFF);
        int firstFwOff = fxOff + fwOffIncr;

        DebugLog.add("STYLE_PARSE", "entryCount=%d strideBase=%d firstFwOff=%d",
                entryCount, strideBase, firstFwOff);

        if (entryCount == 0 || firstFwOff + 8 > recordSize) {
            DebugLog.add("STYLE_PARSE", "no fw entries");
            return result;
        }

        int pos = firstFwOff;
        for (int i = 0; i < entryCount && pos + 8 <= recordSize; i++) {
            int styleId = recordData[pos] & 0xFF;
            int extraStride = recordData[pos + 1] & 0xFF;
            int p1 = read16(recordData, pos + 2);
            int p2 = read16(recordData, pos + 4);
            int p3 = read16(recordData, pos + 6);

            DebugLog.add("STYLE_FW", "  [%d] id=%d p1=%d p2=%d p3=%d extra=%d",
                    i, styleId, p1, p2, p3, extraStride);

            int entryStride = strideBase + ((extraStride + 2) & 0xFFFE);
            if (entryStride < 8) entryStride = 8;
            pos += entryStride;
        }

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
