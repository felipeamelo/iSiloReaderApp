package com.dcco.app.iSilo.engine.render;

import java.util.ArrayList;
import java.util.List;

public class LayoutEngine {

    private CharMeasurer measurer;

    public LayoutEngine(CharMeasurer measurer) {
        this.measurer = measurer;
    }

    public interface LineBreakHandler {
        void onLine(LayoutLine line);
    }

    public void layoutText(FormattedText text, int width, int startOffset, int endOffset, LineBreakHandler handler) {
        if (text == null || text.getTotalLength() == 0) return;
        if (endOffset > text.getTotalLength()) endOffset = text.getTotalLength();

        int lineX = 0;
        int lineY = 0;
        int lineHeight = 0;
        int lineBaseline = 0;
        TextStyle lineStyle = null;

        int runIdx = text.findRunAtOffset(startOffset);
        int offsetInRun = text.getOffsetInRun(startOffset);

        int lineStart = startOffset;
        int wordStart = startOffset;
        int wordWidth = 0;
        int lastBreak = -1;
        int lastBreakWidth = 0;

        for (int i = startOffset; i < endOffset; i++) {
            TextRun run = text.getRun(runIdx);
            TextStyle style = run.style;
            char c = run.charAt(offsetInRun);

            int cw = measurer.measureCharWidth(c, style);
            int lh = measurer.getLineHeight(style);
            int lb = measurer.getBaseline(style);

            if (lh > lineHeight) lineHeight = lh;
            if (lb > lineBaseline) lineBaseline = lb;
            if (lineStyle == null) lineStyle = style;

            if (c == '\n') {
                int len = (lastBreak >= 0 ? lastBreak : i) - lineStart;
                if (len < 0) len = 0;
                int lw = lastBreak >= 0 ? lastBreakWidth : lineX;
                handler.onLine(new LayoutLine(lineStart, len, 0, lineY, lw, lineHeight, lineBaseline, lineStyle));
                lineY += lineHeight;
                lineX = 0;
                lineHeight = 0;
                lineBaseline = 0;
                lineStart = i + 1;
                lineStyle = null;
                lastBreak = -1;
                lastBreakWidth = 0;
                wordStart = i + 1;
                wordWidth = 0;
            } else if (c == ' ' || c == '\t') {
                if (lineX + wordWidth + cw > width && lineStart < wordStart) {
                    int len = wordStart - lineStart - 1;
                    if (len < 0) len = 0;
                    handler.onLine(new LayoutLine(lineStart, len, 0, lineY, lastBreakWidth, lineHeight, lineBaseline, lineStyle));
                    lineY += lineHeight;
                    lineX = wordWidth;
                    lineHeight = lh;
                    lineBaseline = lb;
                    lineStart = wordStart;
                    lineStyle = style;
                    lastBreak = i;
                    lastBreakWidth = lineX;
                } else {
                    lineX += wordWidth + cw;
                    lastBreak = i;
                    lastBreakWidth = lineX;
                }
                wordWidth = 0;
                wordStart = i + 1;
            } else {
                wordWidth += cw;
                if (lineX + wordWidth > width && lineStart < i) {
                    int breakPos = lastBreak > lineStart ? lastBreak : i;
                    int breakW = lastBreak > lineStart ? lastBreakWidth : 0;
                    int len = breakPos - lineStart;
                    if (len < 0) len = 0;
                    handler.onLine(new LayoutLine(lineStart, len, 0, lineY, breakW, lineHeight, lineBaseline, lineStyle));
                    lineY += lineHeight;
                    lineX = lineX - lastBreakWidth + wordWidth;
                    lineHeight = lh;
                    lineBaseline = lb;
                    lineStart = breakPos;
                    if (lineStart == i) {
                        lineX = 0;
                        wordWidth = cw;
                    }
                    lineStyle = style;
                    lastBreak = -1;
                    lastBreakWidth = 0;
                }
            }

            offsetInRun++;
            if (offsetInRun >= run.length) {
                runIdx++;
                if (runIdx < text.getRunCount()) {
                    offsetInRun = 0;
                } else {
                    break;
                }
            }
        }

        int remaining = endOffset - lineStart;
        if (remaining > 0) {
            handler.onLine(new LayoutLine(lineStart, remaining, 0, lineY, lineX, lineHeight, lineBaseline, lineStyle));
            lineY += lineHeight;
        }
    }

    public void layoutText(FormattedText text, int width, List<LayoutLine> lines, int startOffset, int endOffset) {
        layoutText(text, width, startOffset, endOffset, new LineBreakHandler() {
            @Override
            public void onLine(LayoutLine line) {
                lines.add(line);
            }
        });
    }

    public List<LayoutLine> layoutText(FormattedText text, int width, int startOffset, int endOffset) {
        List<LayoutLine> lines = new ArrayList<>();
        layoutText(text, width, lines, startOffset, endOffset);
        return lines;
    }
}
