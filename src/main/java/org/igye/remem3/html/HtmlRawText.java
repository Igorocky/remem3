package org.igye.remem3.html;

import lombok.Getter;

public final class HtmlRawText implements HtmlElem {
    @Getter
    private final String text;

    public HtmlRawText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
