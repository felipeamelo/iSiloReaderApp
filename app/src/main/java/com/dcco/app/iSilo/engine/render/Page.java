package com.dcco.app.iSilo.engine.render;

import java.util.List;

public class Page {
    public int pageIndex;
    public int startOffset;
    public int endOffset;
    public List<LayoutLine> lines;
    public int contentWidth;
    public int contentHeight;

    public Page(int pageIndex, int startOffset, int endOffset, List<LayoutLine> lines, int contentWidth, int contentHeight) {
        this.pageIndex = pageIndex;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.lines = lines;
        this.contentWidth = contentWidth;
        this.contentHeight = contentHeight;
    }

    public int getLineCount() {
        return lines != null ? lines.size() : 0;
    }

    public LayoutLine getLine(int index) {
        return lines != null ? lines.get(index) : null;
    }

    public int findLineAtCharOffset(int charOffset) {
        if (lines == null) return -1;
        for (int i = 0; i < lines.size(); i++) {
            LayoutLine line = lines.get(i);
            if (charOffset >= line.charOffset && charOffset < line.charOffset + line.charLength) {
                return i;
            }
        }
        return -1;
    }
}
