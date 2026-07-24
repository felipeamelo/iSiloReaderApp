package com.dcco.app.iSilo.engine.render;

import java.util.ArrayList;
import java.util.List;

public class DocumentLayout {

    private FormattedText text;
    private CharMeasurer measurer;
    private LayoutEngine engine;
    private List<Page> pages;
    private List<LayoutLine> allLines;
    private int pageWidth;
    private int pageHeight;
    private boolean pageMode;
    private int nextCharOffset;
    private boolean layoutDone;
    private static final int CHARS_PER_CHUNK = 2500;

    private int[] fixedPageOffsets;

    public DocumentLayout(FormattedText text, CharMeasurer measurer) {
        this.text = text;
        this.measurer = measurer;
        this.engine = new LayoutEngine(measurer);
        this.pages = new ArrayList<>();
        this.allLines = new ArrayList<>();
        this.pageMode = true;
        this.nextCharOffset = 0;
        this.layoutDone = false;
    }

    public void setPageOffsets(int[] offsets) {
        this.fixedPageOffsets = offsets;
    }

    public void setPageMode(boolean pageMode) {
        this.pageMode = pageMode;
    }

    public boolean isPageMode() {
        return pageMode;
    }

    public void layout(int width, int height) {
        this.pageWidth = width;
        this.pageHeight = height;
        this.pages.clear();
        this.allLines.clear();
        this.nextCharOffset = 0;
        this.layoutDone = (fixedPageOffsets != null && fixedPageOffsets.length > 0);
        if (fixedPageOffsets != null && fixedPageOffsets.length > 0) {
            this.pages.clear();
            for (int i = 0; i < fixedPageOffsets.length; i++) {
                pages.add(null);
            }
        }
    }

    public int getPageCount() {
        if (fixedPageOffsets != null && fixedPageOffsets.length > 0) {
            return fixedPageOffsets.length;
        }
        return pages.size();
    }

    public boolean isFullyLaidOut() {
        return layoutDone;
    }

    public Page getPage(int index) {
        if (fixedPageOffsets != null && fixedPageOffsets.length > 0) {
            return buildFixedPage(index);
        }
        ensurePage(index);
        if (!layoutDone) {
            layoutChunkToBuildPage();
        }
        return index < pages.size() ? pages.get(index) : null;
    }

    private Page buildFixedPage(int index) {
        if (index < 0 || index >= fixedPageOffsets.length) return null;
        if (pages.get(index) != null) return pages.get(index);

        int startOff = fixedPageOffsets[index];
        int endOff = (index + 1 < fixedPageOffsets.length)
                ? fixedPageOffsets[index + 1]
                : text.getTotalLength();
        if (endOff <= startOff) return null;

        List<LayoutLine> lines = engine.layoutText(text, pageWidth, startOff, endOff);
        if (lines.isEmpty()) return null;

        int y = 0;
        List<LayoutLine> adjusted = new ArrayList<>();
        for (LayoutLine src : lines) {
            adjusted.add(new LayoutLine(
                    src.charOffset, src.charLength,
                    0, y, src.width, src.height, src.baseline, src.style
            ));
            y += src.height;
        }

        Page page = new Page(index, startOff, endOff, adjusted, pageWidth, y);
        pages.set(index, page);
        return page;
    }

    private void ensurePage(int index) {
        while (pages.size() <= index && !layoutDone) {
            layoutChunkToBuildPage();
        }
    }

    private void layoutChunkToBuildPage() {
        if (layoutDone) return;

        int totalLen = text.getTotalLength();
        int chunkEnd = Math.min(nextCharOffset + CHARS_PER_CHUNK, totalLen);
        if (chunkEnd <= nextCharOffset) { layoutDone = true; return; }

        int startLineIdx = allLines.size();
        List<LayoutLine> newLines = engine.layoutText(text, pageWidth, nextCharOffset, chunkEnd);
        if (newLines.isEmpty()) { layoutDone = true; return; }

        allLines.addAll(newLines);
        LayoutLine last = newLines.get(newLines.size() - 1);
        nextCharOffset = last.charOffset + last.charLength;
        if (nextCharOffset >= totalLen) layoutDone = true;

        buildPagesFromLines(startLineIdx);
    }

    private void buildPagesFromLines(int fromLineIdx) {
        int totalLines = allLines.size();
        int lineIdx = fromLineIdx;

        while (lineIdx < totalLines) {
            int y = 0;
            List<LayoutLine> pageLines = new ArrayList<>();

            while (lineIdx < totalLines) {
                LayoutLine src = allLines.get(lineIdx);
                int nextY = y + src.height;
                if (nextY > pageHeight && pageLines.size() > 0) break;
                LayoutLine adjusted = new LayoutLine(
                        src.charOffset, src.charLength,
                        0, y, src.width, src.height, src.baseline, src.style
                );
                pageLines.add(adjusted);
                y = nextY;
                lineIdx++;
            }

            if (pageLines.isEmpty()) break;

            int firstOff = pageLines.get(0).charOffset;
            int lastOff = pageLines.get(pageLines.size() - 1).charOffset
                    + pageLines.get(pageLines.size() - 1).charLength;
            pages.add(new Page(pages.size(), firstOff, lastOff, pageLines, pageWidth, y));
        }
    }

    public int findPageAtOffset(int charOffset) {
        if (fixedPageOffsets != null && fixedPageOffsets.length > 0) {
            for (int i = fixedPageOffsets.length - 1; i >= 0; i--) {
                if (charOffset >= fixedPageOffsets[i]) return i;
            }
            return 0;
        }
        ensurePage(100);
        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            if (charOffset >= page.startOffset && charOffset < page.endOffset) {
                return i;
            }
        }
        if (!pages.isEmpty() && charOffset >= pages.get(pages.size() - 1).endOffset) {
            return pages.size() - 1;
        }
        return 0;
    }

    public FormattedText getText() {
        return text;
    }

    public void setText(FormattedText text) {
        this.text = text;
        this.pages.clear();
        this.allLines.clear();
        this.nextCharOffset = 0;
        this.layoutDone = false;
    }

    public int getPageWidth() {
        return pageWidth;
    }

    public int getPageHeight() {
        return pageHeight;
    }

    public void invalidate() {
        pages.clear();
        allLines.clear();
        nextCharOffset = 0;
        layoutDone = (fixedPageOffsets != null && fixedPageOffsets.length > 0);
    }
}
