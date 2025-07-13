package org.igye.remem3.html;

import lombok.Getter;

public final class HtmlText implements HtmlElem {
    @Getter
    private final String text;

    public HtmlText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
