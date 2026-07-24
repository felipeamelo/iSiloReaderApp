package com.dcco.app.iSilo.engine.render;

public class TextStyle {
    public int fontId;
    public int fontSize;
    public int fontColor;
    public int bgColor;
    public int typeface;
    public boolean bold;
    public boolean italic;
    public boolean underline;
    public boolean strikeThrough;
    public boolean superscript;
    public boolean subscript;

    public static final int FACEDEFAULT = 0;
    public static final int FACESERIF = 1;
    public static final int FACESANS = 2;
    public static final int FACEMONO = 3;

    public static final int DEFAULT_FONT_SIZE = 20;
    public static final int DEFAULT_FONT_COLOR = 0xFF000000;
    public static final int DEFAULT_BG_COLOR = 0;

    public TextStyle() {
        this.fontSize = DEFAULT_FONT_SIZE;
        this.fontColor = DEFAULT_FONT_COLOR;
    }

    public TextStyle(TextStyle other) {
        this.fontId = other.fontId;
        this.fontSize = other.fontSize;
        this.fontColor = other.fontColor;
        this.bgColor = other.bgColor;
        this.typeface = other.typeface;
        this.bold = other.bold;
        this.italic = other.italic;
        this.underline = other.underline;
        this.strikeThrough = other.strikeThrough;
        this.superscript = other.superscript;
        this.subscript = other.subscript;
    }

    public void copyFrom(TextStyle other) {
        this.fontId = other.fontId;
        this.fontSize = other.fontSize;
        this.fontColor = other.fontColor;
        this.bgColor = other.bgColor;
        this.typeface = other.typeface;
        this.bold = other.bold;
        this.italic = other.italic;
        this.underline = other.underline;
        this.strikeThrough = other.strikeThrough;
        this.superscript = other.superscript;
        this.subscript = other.subscript;
    }

    public int getLineHeight() {
        int h = fontSize + 4;
        if (superscript || subscript) h = h * 3 / 4;
        return h;
    }

    public int getBaselineOffset() {
        if (superscript) return -fontSize / 3;
        if (subscript) return fontSize / 3;
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TextStyle)) return false;
        TextStyle t = (TextStyle) o;
        return fontId == t.fontId && fontSize == t.fontSize
                && fontColor == t.fontColor && bgColor == t.bgColor
                && typeface == t.typeface
                && bold == t.bold && italic == t.italic
                && underline == t.underline && strikeThrough == t.strikeThrough
                && superscript == t.superscript && subscript == t.subscript;
    }

    @Override
    public int hashCode() {
        int h = fontId;
        h = h * 31 + fontSize;
        h = h * 31 + fontColor;
        h = h * 31 + bgColor;
        h = h * 31 + typeface;
        h = h * 31 + (bold ? 1 : 0);
        h = h * 31 + (italic ? 1 : 0);
        h = h * 31 + (underline ? 1 : 0);
        h = h * 31 + (strikeThrough ? 1 : 0);
        h = h * 31 + (superscript ? 1 : 0);
        h = h * 31 + (subscript ? 1 : 0);
        return h;
    }
}
