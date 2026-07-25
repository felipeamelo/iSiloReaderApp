package com.dcco.app.iSilo.ui.reader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import com.dcco.app.iSilo.engine.format.DocFormat;
import com.dcco.app.iSilo.engine.format.iSiloDocInfo;
import com.dcco.app.iSilo.engine.format.LinkEntry;
import com.dcco.app.iSilo.engine.render.*;
import com.dcco.app.iSilo.engine.util.AppLog;

public class ReadView extends View {

    private DocumentLayout layout;
    private AndroidCharMeasurer measurer;
    private DocFormat doc;
    private FormattedText formattedText;
    private int currentPage;

    private Paint textPaint;
    private Paint bgPaint;
    private Paint headerPaint;
    private Paint versionPaint;

    public ReadView(Context context) {
        super(context);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        measurer = new AndroidCharMeasurer(density);

        bgPaint = new Paint();
        bgPaint.setColor(0xFFFFF8DC);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        headerPaint = new Paint();
        headerPaint.setColor(0xFF666666);
        headerPaint.setTextSize(14 * density);
        headerPaint.setAntiAlias(true);

        versionPaint = new Paint();
        versionPaint.setColor(0xFFAAAAAA);
        versionPaint.setTextSize(11 * density);
        versionPaint.setAntiAlias(true);

        currentPage = 0;
    }

    public void openDocument(DocFormat doc) {
        this.doc = doc;
        formattedText = textToFormatted(doc);
        if (formattedText != null) {
            layout = new DocumentLayout(formattedText, measurer);
            layout.setPageMode(true);
            if (doc.getInfo() != null && doc.getInfo().pageOffsets != null
                    && doc.getInfo().pageOffsets.length > 0) {
                layout.setPageOffsets(doc.getInfo().pageOffsets);
            }
            buildLinksFromTOC();
            requestLayout();
        }
    }

    private void buildLinksFromTOC() {
        if (doc == null || doc.getInfo() == null || formattedText == null) return;
        iSiloDocInfo info = doc.getInfo();
        if (info.tocTitles == null || info.tocOffsets == null) return;
        if (info.links == null) info.links = new java.util.ArrayList<>();

        String fullText = formattedText.getPlainText();
        for (int i = 0; i < info.tocTitles.length && i < info.tocOffsets.length; i++) {
            String title = info.tocTitles[i];
            if (title == null || title.isEmpty()) continue;
            int idx = fullText.indexOf(title);
            if (idx >= 0) {
                LinkEntry link = new LinkEntry(idx, info.tocOffsets[i], title.length(), title);
                info.links.add(link);
                AppLog.add("TOC_LINK", "title='%s' charOff=%d target=%d", title, idx, info.tocOffsets[i]);
            }
        }
        AppLog.add("TOC_LINK", "built %d link entries from TOC", info.links.size());
    }

    private FormattedText textToFormatted(DocFormat doc) {
        iSiloDocInfo info = doc.getInfo();
        AppLog.add("TEXT_TO_FMT", "info=%s", info);
        if (info == null) {
            android.widget.Toast.makeText(getContext(), "getInfo() == null", android.widget.Toast.LENGTH_LONG).show();
            return null;
        }

        int textSize = info.textSize;
        AppLog.add("TEXT_TO_FMT", "textSize=%d", textSize);
        if (textSize <= 0) {
            android.widget.Toast.makeText(getContext(), "textSize=" + textSize, android.widget.Toast.LENGTH_LONG).show();
            return null;
        }

        byte[] buffer = new byte[textSize];
        int res = doc.getText(0, textSize, buffer);
        AppLog.add("TEXT_TO_FMT", "getText(0,%d) returned %d", textSize, res);
        if (res < 0) {
            android.widget.Toast.makeText(getContext(), "getText() erro: " + res, android.widget.Toast.LENGTH_LONG).show();
            return null;
        }
        AppLog.hex("TEXT_TO_FMT_BUF", buffer, 0, Math.min(40, res));

        StyleResolver resolver = new StyleResolver();

        if (info.fontTable != null) {
            resolver.setFontTable(info.fontTable);
            resolver.setDefaultStyleId(0);
        }

        byte[] styleData = info.styleData;
        int styleDataLen = (styleData != null) ? styleData.length : 0;
        AppLog.add("STYLE_PASS", "styleDataLen=%d fontTable=%s", styleDataLen, info.fontTable != null ? "set" : "null");

        int charset = info != null ? info.charset : 0;
        FormattedText ft = resolver.resolveText(buffer, textSize, styleData, styleDataLen, null, 0, charset);
        AppLog.add("TEXT_TO_FMT", "resolveText: charset=%d ft=%s totalLen=%d", charset, ft, ft != null ? ft.getTotalLength() : -1);
        if (ft == null || ft.getTotalLength() == 0) {
            android.widget.Toast.makeText(getContext(), "Texto vazio após resolver", android.widget.Toast.LENGTH_LONG).show();
            return null;
        }
        if (ft != null && ft.getTotalLength() > 0) {
            AppLog.add("TEXT_TO_FMT", "runCount=%d firstChars='%s'", ft.getRunCount(), ft.getPlainText().substring(0, Math.min(40, ft.getTotalLength())));
        }
        return ft;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (layout != null && w > 0 && h > 0) {
            try {
                int padLeft = getPaddingLeft();
                int padTop = getPaddingTop();
                int padRight = getPaddingRight();
                int padBottom = getPaddingBottom();
                int contentW = w - padLeft - padRight;
                int contentH = h - padTop - padBottom;
                if (contentW > 0 && contentH > 0) {
                    layout.layout(contentW, contentH);
                    if (currentPage >= layout.getPageCount()) currentPage = 0;
                    invalidate();
                }
            } catch (Exception e) {
                layout = null;
                formattedText = null;
                android.util.Log.e("ReadView", "layout error", e);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        try {
            drawContent(canvas);
        } catch (Exception e) {
            String err = "onDraw error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            android.util.Log.e("ReadView", err, e);
            Paint p = new Paint();
            p.setColor(0xFFFF0000);
            p.setTextSize(24 * getResources().getDisplayMetrics().density);
            canvas.drawText(err, 50, 150, p);
        }
    }

    private void drawContent(Canvas canvas) {
        canvas.drawPaint(bgPaint);

        if (layout == null || doc == null || formattedText == null) {
            float density = getResources().getDisplayMetrics().density;
            Paint p = new Paint();
            p.setColor(0xFF888888);
            p.setTextSize(24 * density);
            canvas.drawText("Nenhum documento aberto", 50, 100, p);
            return;
        }

        int padLeft = getPaddingLeft();
        int padTop = getPaddingTop();

        Page page = layout.getPage(currentPage);
        if (page == null) return;

        int pageCount = layout.getPageCount();
        int headerY = padTop + Math.round(headerPaint.getTextSize());
        String footer;
        if (layout.isFullyLaidOut()) {
            footer = String.format("Página %d/%d", currentPage + 1, pageCount);
        } else {
            footer = String.format("Página %d", currentPage + 1);
        }
        canvas.drawText(footer, padLeft, headerY, headerPaint);

        if (doc.getInfo() != null && doc.getInfo().title != null) {
            float titleW = headerPaint.measureText(doc.getInfo().title);
            canvas.drawText(doc.getInfo().title,
                    getWidth() - getPaddingRight() - titleW,
                    headerY, headerPaint);
        }

        String versionStr = "v1.1.1";
        float versionW = versionPaint.measureText(versionStr);
        canvas.drawText(versionStr, getWidth() - getPaddingRight() - versionW, getHeight() - getPaddingBottom(), versionPaint);

        int contentTop = (int)(headerY + headerPaint.getTextSize() + 8);

        for (int li = 0; li < page.getLineCount(); li++) {
            LayoutLine line = page.getLine(li);
            if (line == null || line.charLength <= 0) continue;

            int y = contentTop + line.y + line.baseline;

            int lineStart = line.charOffset;
            int lineEnd = lineStart + line.charLength;

            int ri = formattedText.findRunAtOffset(lineStart);

            while (ri < formattedText.getRunCount()) {
                TextRun run = formattedText.getRun(ri);
                int runStart = runGlobalOffset(formattedText, ri);
                int runEnd = runStart + run.length;

                if (runEnd <= lineStart) { ri++; continue; }
                if (runStart >= lineEnd) break;

                int segStart = Math.max(lineStart, runStart);
                int segEnd = Math.min(lineEnd, runEnd);
                int segLen = segEnd - segStart;

                int localRunOffset = segStart - runStart;
                int x = padLeft + line.x + (segStart - lineStart);

                applyStyle(textPaint, run.style);
                if (run.style.bgColor != 0 && run.style.bgColor != 0xFF000000) {
                    float tw = textPaint.measureText(run.text, run.offset + localRunOffset, segLen);
                    Paint bgP = new Paint();
                    bgP.setColor(run.style.bgColor);
                    canvas.drawRect(x, y - textPaint.getFontMetrics().ascent,
                            x + tw, y + textPaint.getFontMetrics().descent, bgP);
                }
                canvas.drawText(run.text, run.offset + localRunOffset, segLen,
                        x, y, textPaint);

                ri++;
            }
        }
    }

    private int runGlobalOffset(FormattedText ft, int runIndex) {
        int offset = 0;
        for (int i = 0; i < runIndex; i++) {
            offset += ft.getRun(i).length;
        }
        return offset;
    }

    private Typeface getTypefaceBase(int tf) {
        switch (tf) {
            case TextStyle.FACESERIF: return Typeface.SERIF;
            case TextStyle.FACESANS: return Typeface.SANS_SERIF;
            case TextStyle.FACEMONO: return Typeface.MONOSPACE;
            default: return Typeface.DEFAULT;
        }
    }

    private void applyStyle(Paint paint, TextStyle style) {
        Typeface base = getTypefaceBase(style.typeface);
        int styleFlags = Typeface.NORMAL;
        if (style.bold && style.italic) styleFlags = Typeface.BOLD_ITALIC;
        else if (style.bold) styleFlags = Typeface.BOLD;
        else if (style.italic) styleFlags = Typeface.ITALIC;
        paint.setTypeface(Typeface.create(base, styleFlags));
        paint.setTextSize(style.fontSize * getResources().getDisplayMetrics().density);
        paint.setColor(style.fontColor);
        paint.setUnderlineText(style.underline);
        paint.setStrikeThruText(style.strikeThrough);
        paint.setAntiAlias(true);
    }

    public boolean nextPage() {
        if (layout == null) return false;
        if (currentPage + 1 < layout.getPageCount()) {
            currentPage++;
            invalidate();
            return true;
        }
        return false;
    }

    public boolean previousPage() {
        if (currentPage > 0) {
            currentPage--;
            invalidate();
            return true;
        }
        return false;
    }

    public void goToPage(int page) {
        if (layout != null && page >= 0 && page < layout.getPageCount()) {
            currentPage = page;
            invalidate();
        }
    }

    public int getCurrentPage() { return currentPage; }
    public int getPageCount() { return layout != null ? layout.getPageCount() : 0; }

    public int getCurrentPageStartOffset() {
        if (layout == null) return 0;
        Page page = layout.getPage(currentPage);
        return page != null ? page.startOffset : 0;
    }

    public int findPageAtOffset(int charOffset) {
        if (layout == null) return 0;
        return layout.findPageAtOffset(charOffset);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float width = getWidth();
            if (x < width / 3) {
                previousPage();
                return true;
            } else if (x > 2 * width / 3) {
                nextPage();
                return true;
            }
            if (doc != null && doc.getInfo() != null && doc.getInfo().links != null && !doc.getInfo().links.isEmpty()) {
                handleLinkTap(event);
                return true;
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void handleLinkTap(MotionEvent event) {
        if (layout == null || formattedText == null) return;
        Page page = layout.getPage(currentPage);
        if (page == null) return;

        float tapX = event.getX() - getPaddingLeft();
        float tapY = event.getY() - getPaddingTop();
        int headerH = Math.round(headerPaint.getTextSize() * 2.5f);
        tapY -= headerH;
        if (tapY < 0) return;

        int charOffset = -1;
        for (int li = 0; li < page.getLineCount(); li++) {
            LayoutLine line = page.getLine(li);
            if (line == null) continue;
            if (tapY >= line.y && tapY < line.y + line.height) {
                float relX = tapX - (getPaddingLeft() + line.x);
                if (relX >= 0) {
                    int ri = formattedText.findRunAtOffset(line.charOffset);
                    if (ri < formattedText.getRunCount()) {
                        TextRun run = formattedText.getRun(ri);
                        int runOff = line.charOffset - (ri > 0 ? runGlobalOffset(formattedText, ri) : 0);
                        charOffset = line.charOffset + (int)(relX / (run.style.fontSize * getResources().getDisplayMetrics().density * 0.6f));
                        if (charOffset >= line.charOffset + line.charLength) {
                            charOffset = line.charOffset + line.charLength - 1;
                        }
                    }
                }
                break;
            }
        }

        if (charOffset >= 0) {
            java.util.ArrayList<LinkEntry> links = doc.getInfo().links;
            for (LinkEntry link : links) {
                if (charOffset >= link.charOffset && charOffset < link.charOffset + link.length) {
                    int targetPage = layout.findPageAtOffset(link.targetOffset);
                    goToPage(targetPage);
                    android.widget.Toast.makeText(getContext(), link.title != null && !link.title.isEmpty()
                            ? link.title : "Ir para offset " + link.targetOffset,
                            android.widget.Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }
    }
}
