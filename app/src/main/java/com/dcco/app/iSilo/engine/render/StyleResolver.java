package com.dcco.app.iSilo.engine.render;

import com.dcco.app.iSilo.engine.format.iSiloDocInfo;
import com.dcco.app.iSilo.engine.util.ErrorUtil;

import java.util.ArrayList;

public class StyleResolver {

    private TextStyle[] fontTable;
    private int defaultStyleId;

    public StyleResolver() {
        initDefaultFonts();
    }

    private void initDefaultFonts() {
        fontTable = new TextStyle[32];
        for (int i = 0; i < 32; i++) {
            fontTable[i] = new TextStyle();
        }

        fontTable[0].fontSize = 20;
        fontTable[1].fontSize = 20; fontTable[1].bold = true;
        fontTable[2].fontSize = 20; fontTable[2].italic = true;
        fontTable[3].fontSize = 20; fontTable[3].bold = true; fontTable[3].italic = true;
        fontTable[4].fontSize = 14;
        fontTable[5].fontSize = 14; fontTable[5].bold = true;
        fontTable[6].fontSize = 14; fontTable[6].italic = true;
        fontTable[7].fontSize = 14; fontTable[7].bold = true; fontTable[7].italic = true;
        fontTable[8].fontSize = 28;
        fontTable[9].fontSize = 28; fontTable[9].bold = true;
        fontTable[10].fontSize = 24;
        fontTable[11].fontSize = 24; fontTable[11].bold = true;
        fontTable[12].fontSize = 18;
        fontTable[13].fontSize = 18; fontTable[13].bold = true;
        fontTable[14].fontSize = 26;
        fontTable[15].fontSize = 26; fontTable[15].bold = true;

        for (int i = 16; i < 32; i++) {
            fontTable[i].fontSize = 16;
            fontTable[i].bold = (i & 1) != 0;
            fontTable[i].italic = (i & 2) != 0;
            fontTable[i].underline = (i & 4) != 0;
        }
    }

    public void setFontTable(TextStyle[] table) {
        if (table != null) {
            int len = Math.min(table.length, fontTable.length);
            for (int i = 0; i < len; i++) {
                if (table[i] != null) fontTable[i] = table[i];
            }
        }
    }

    public void setDefaultStyleId(int id) {
        if (id >= 0 && id < fontTable.length) defaultStyleId = id;
    }

    public TextStyle getStyle(int styleId) {
        if (styleId >= 0 && styleId < fontTable.length) return fontTable[styleId];
        return fontTable[0];
    }

    public int getDefaultStyleId() {
        return defaultStyleId;
    }

    private static class StyleSpan {
        int offset;
        int styleId;
        StyleSpan(int offset, int styleId) {
            this.offset = offset;
            this.styleId = styleId;
        }
    }

    private static final char[] WIN1252_MAP = {
        0x20AC, 0xFFFD, 0x201A, 0x0192, 0x201E, 0x2026, 0x2020, 0x2021,
        0x02C6, 0x2030, 0x0160, 0x2039, 0x0152, 0xFFFD, 0x017D, 0xFFFD,
        0xFFFD, 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013, 0x2014,
        0x02DC, 0x2122, 0x0161, 0x203A, 0x0153, 0xFFFD, 0x017E, 0x0178
    };

    public FormattedText resolveText(byte[] rawText, int textLen,
                                      byte[] styleData, int styleLen,
                                      byte[] linkData, int linkLen) {
        return resolveText(rawText, textLen, styleData, styleLen, linkData, linkLen,
                iSiloDocInfo.CHARSET_LATIN1);
    }

    public FormattedText resolveText(byte[] rawText, int textLen,
                                      byte[] styleData, int styleLen,
                                      byte[] linkData, int linkLen,
                                      int charset) {
        FormattedText result = new FormattedText();

        if (rawText == null || textLen <= 0) return result;

        char[] chars;
        int charLen;

        boolean useUTF8 = (charset == iSiloDocInfo.CHARSET_UTF8);
        if (!useUTF8) {
            useUTF8 = detectUTF8(rawText, textLen);
        }

        if (useUTF8) {
            String decoded = decodeUTF8(rawText, textLen);
            chars = decoded.toCharArray();
            charLen = chars.length;
        } else {
            chars = new char[textLen];
            for (int i = 0; i < textLen; i++) {
                int b = rawText[i] & 0xFF;
                if (b >= 0x80 && b <= 0x9F) {
                    chars[i] = WIN1252_MAP[b - 0x80];
                } else {
                    chars[i] = (char) b;
                }
            }
            charLen = textLen;
        }

        ArrayList<StyleSpan> spans = new ArrayList<>();
        if (styleData != null && styleLen >= 4) {
            int pos = 0;
            while (pos + 4 <= styleLen) {
                int offset = ((styleData[pos] & 0xFF) << 8) | (styleData[pos + 1] & 0xFF);
                int styleId = ((styleData[pos + 2] & 0xFF) << 8) | (styleData[pos + 3] & 0xFF);
                if (offset > charLen) break;
                spans.add(new StyleSpan(offset, styleId));
                pos += 4;
            }
        }

        if (spans.isEmpty()) {
            TextStyle style = fontTable[defaultStyleId];
            TextRun run = new TextRun(chars, 0, charLen, style);
            result.addRun(run);
        } else {
            int currentOffset = 0;
            TextStyle currentStyle = fontTable[defaultStyleId];

            for (StyleSpan span : spans) {
                if (span.offset > currentOffset) {
                    int len = span.offset - currentOffset;
                    result.addRun(new TextRun(chars, currentOffset, len, currentStyle));
                    currentOffset = span.offset;
                }
                currentStyle = getStyle(span.styleId);
            }

            if (currentOffset < charLen) {
                result.addRun(new TextRun(chars, currentOffset, charLen - currentOffset, currentStyle));
            }
        }

        return result;
    }

    private static boolean detectUTF8(byte[] data, int len) {
        int checkLen = Math.min(len, 500);
        int multiByteSeqs = 0;
        int invalidBytes = 0;
        int i = 0;
        while (i < checkLen) {
            int b = data[i] & 0xFF;
            if (b >= 0xC0 && b < 0xE0) {
                if (i + 1 < checkLen && (data[i + 1] & 0xC0) == 0x80) {
                    multiByteSeqs++;
                    i += 2;
                } else { invalidBytes++; i++; }
            } else if (b >= 0xE0 && b < 0xF0) {
                if (i + 2 < checkLen && (data[i + 1] & 0xC0) == 0x80 && (data[i + 2] & 0xC0) == 0x80) {
                    multiByteSeqs++;
                    i += 3;
                } else { invalidBytes++; i++; }
            } else if (b >= 0xF0 && b < 0xF8) {
                if (i + 3 < checkLen && (data[i + 1] & 0xC0) == 0x80 && (data[i + 2] & 0xC0) == 0x80 && (data[i + 3] & 0xC0) == 0x80) {
                    multiByteSeqs++;
                    i += 4;
                } else { invalidBytes++; i++; }
            } else if (b >= 0x80 && b < 0xC0) {
                invalidBytes++;
                i++;
            } else {
                i++;
            }
        }
        return multiByteSeqs > 0 && invalidBytes < multiByteSeqs;
    }

    private static String decodeUTF8(byte[] data, int len) {
        StringBuilder sb = new StringBuilder(len);
        int i = 0;
        while (i < len) {
            int b = data[i] & 0xFF;
            if (b < 0x80) {
                sb.append((char) b);
                i++;
            } else if (b >= 0xC0 && b < 0xE0 && i + 1 < len) {
                int c = ((b & 0x1F) << 6) | (data[i + 1] & 0x3F);
                sb.append((char) c);
                i += 2;
            } else if (b >= 0xE0 && b < 0xF0 && i + 2 < len) {
                int c = ((b & 0x0F) << 12) | ((data[i + 1] & 0x3F) << 6) | (data[i + 2] & 0x3F);
                sb.append((char) c);
                i += 3;
            } else if (b >= 0xF0 && b < 0xF8 && i + 3 < len) {
                int c = ((b & 0x07) << 18) | ((data[i + 1] & 0x3F) << 12) | ((data[i + 2] & 0x3F) << 6) | (data[i + 3] & 0x3F);
                if (c > 0xFFFF) {
                    sb.append((char) (0xD800 + ((c - 0x10000) >> 10)));
                    sb.append((char) (0xDC00 + ((c - 0x10000) & 0x3FF)));
                } else {
                    sb.append((char) c);
                }
                i += 4;
            } else {
                sb.append('?');
                i++;
            }
        }
        return sb.toString();
    }
}
