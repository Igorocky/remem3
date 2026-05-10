package org.igye.remem3.app.controllers.index;

import org.igye.remem3.app.state.StateRenderer;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;

public class IndexRenderer extends HtmlBuilder implements StateRenderer<IndexState> {
    @Override
    public HtmlElem render(IndexState state) {
        return simplePageWithTitle("Index",
            state.getStateConstructors().stream()
                .map(c -> div(a("/state/" + c.getName(), text(c.getDisplayName()))))
                .toList()
        );
    }
}
