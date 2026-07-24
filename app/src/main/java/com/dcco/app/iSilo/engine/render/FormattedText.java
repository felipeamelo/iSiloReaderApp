package com.dcco.app.iSilo.engine.render;

import java.util.ArrayList;
import java.util.List;

public class FormattedText {
    private List<TextRun> runs;
    private int totalLength;

    public FormattedText() {
        this.runs = new ArrayList<>();
        this.totalLength = 0;
    }

    public void addRun(TextRun run) {
        runs.add(run);
        totalLength += run.length;
    }

    public int getRunCount() {
        return runs.size();
    }

    public TextRun getRun(int index) {
        return runs.get(index);
    }

    public int getTotalLength() {
        return totalLength;
    }

    public int findRunAtOffset(int charOffset) {
        int pos = 0;
        for (int i = 0; i < runs.size(); i++) {
            TextRun run = runs.get(i);
            if (charOffset < pos + run.length) return i;
            pos += run.length;
        }
        return runs.size() - 1;
    }

    public int getOffsetInRun(int charOffset) {
        int pos = 0;
        for (int i = 0; i < runs.size(); i++) {
            TextRun run = runs.get(i);
            if (charOffset < pos + run.length) {
                return charOffset - pos;
            }
            pos += run.length;
        }
        return 0;
    }

    public char charAt(int offset) {
        int pos = 0;
        for (int i = 0; i < runs.size(); i++) {
            TextRun run = runs.get(i);
            if (offset < pos + run.length) {
                return run.charAt(offset - pos);
            }
            pos += run.length;
        }
        return 0;
    }

    public void appendText(String text, TextStyle style) {
        if (text == null || text.isEmpty()) return;
        if (!runs.isEmpty()) {
            TextRun last = runs.get(runs.size() - 1);
            if (last.style.equals(style) && last.offset + last.length == last.text.length) {
                char[] newText = new char[last.text.length + text.length()];
                System.arraycopy(last.text, 0, newText, 0, last.text.length);
                text.getChars(0, text.length(), newText, last.text.length);
                last.text = newText;
                last.length = newText.length;
                totalLength += text.length();
                return;
            }
        }
        addRun(new TextRun(text, style));
    }

    public String getPlainText() {
        StringBuilder sb = new StringBuilder(totalLength);
        for (int i = 0; i < runs.size(); i++) {
            TextRun run = runs.get(i);
            sb.append(run.text, run.offset, run.length);
        }
        return sb.toString();
    }
}
