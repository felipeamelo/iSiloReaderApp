package com.dcco.app.iSilo.engine.render;

public class TextRun {
    public TextStyle style;
    public char[] text;
    public int offset;
    public int length;

    public TextRun(char[] text, int offset, int length, TextStyle style) {
        this.text = text;
        this.offset = offset;
        this.length = length;
        this.style = style;
    }

    public TextRun(String text, TextStyle style) {
        this.text = text.toCharArray();
        this.offset = 0;
        this.length = this.text.length;
        this.style = style;
    }

    public char charAt(int index) {
        return text[offset + index];
    }

    public TextRun subRun(int start, int len) {
        return new TextRun(text, offset + start, len, style);
    }

    public String getString() {
        return new String(text, offset, length);
    }
}
