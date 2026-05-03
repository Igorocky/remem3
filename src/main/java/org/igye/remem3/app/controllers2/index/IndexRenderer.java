package org.igye.remem3.app.controllers2.index;

import org.igye.remem3.app.state.StateRenderer;
import org.igye.remem3.html.HtmlBuilder;

public class IndexRenderer extends HtmlBuilder implements StateRenderer<IndexState> {
    @Override
    public String render(IndexState state) {
        return simplePageWithTitle("Index",
            state.getAllConstructorNames().stream()
                .map(name -> div(text(name)))
                .toList()
        ).toString();
    }

}
