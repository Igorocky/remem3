package org.igye.remem3.app.controllers2.counter;

import org.igye.remem3.app.state.StateRenderer;
import org.igye.remem3.html.HtmlBuilder;

public class CounterRenderer extends HtmlBuilder implements StateRenderer<CounterState> {
    public static final String ACT_INC = "ACT_INC";

    @Override
    public String render(CounterState state) {
        return simplePageWithTitle("Counter",
            form(inpSubmit(ACT_INC, String.valueOf(state.getCount())))
        ).toString();
    }

}
