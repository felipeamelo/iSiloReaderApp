package com.dcco.app.iSilo.engine.render;

import android.graphics.Paint;
import android.graphics.Typeface;

import java.util.HashMap;

public class AndroidCharMeasurer implements CharMeasurer {

    private Paint paint;
    private float density;
    private HashMap<Integer, Typeface> typefaceCache;

    public AndroidCharMeasurer(float density) {
        this.density = density;
        this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(20 * density);
        this.typefaceCache = new HashMap<>();
    }

    private void applyStyle(TextStyle style) {
        int tfKey = (style.bold ? 1 : 0) | (style.italic ? 2 : 0);
        Typeface tf = typefaceCache.get(tfKey);
        if (tf == null) {
            int styleFlags = Typeface.NORMAL;
            if (style.bold && style.italic) styleFlags = Typeface.BOLD_ITALIC;
            else if (style.bold) styleFlags = Typeface.BOLD;
            else if (style.italic) styleFlags = Typeface.ITALIC;
            tf = Typeface.create(Typeface.DEFAULT, styleFlags);
            typefaceCache.put(tfKey, tf);
        }
        paint.setTypeface(tf);
        paint.setTextSize(style.fontSize * density);
    }

    @Override
    public int measureCharWidth(char c, TextStyle style) {
        applyStyle(style);
        return (int) paint.measureText(new char[]{c}, 0, 1);
    }

    @Override
    public int measureTextWidth(char[] text, int offset, int length, TextStyle style) {
        applyStyle(style);
        return (int) paint.measureText(text, offset, length);
    }

    @Override
    public int getLineHeight(TextStyle style) {
        applyStyle(style);
        Paint.FontMetrics fm = paint.getFontMetrics();
        return (int) (Math.ceil(fm.descent - fm.ascent) + 2);
    }

    @Override
    public int getBaseline(TextStyle style) {
        applyStyle(style);
        Paint.FontMetrics fm = paint.getFontMetrics();
        return (int) Math.ceil(-fm.ascent);
    }

    @Override
    public int getSpaceWidth(TextStyle style) {
        return measureCharWidth(' ', style);
    }

    public Paint getPaint() {
        return paint;
    }
}
