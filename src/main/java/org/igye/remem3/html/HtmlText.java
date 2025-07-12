package org.igye.remem3.html;

public final class HtmlText implements HtmlElem {
    private final String text;

    public HtmlText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
