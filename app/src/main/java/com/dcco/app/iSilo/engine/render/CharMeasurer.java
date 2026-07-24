package com.dcco.app.iSilo.engine.render;

public interface CharMeasurer {
    int measureCharWidth(char c, TextStyle style);
    int measureTextWidth(char[] text, int offset, int length, TextStyle style);
    int getLineHeight(TextStyle style);
    int getBaseline(TextStyle style);
    int getSpaceWidth(TextStyle style);
}
