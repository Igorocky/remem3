package org.igye.remem3.app.controllers2.beans;

import org.igye.remem3.app.state.StateRenderer;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;

public class BeansRenderer extends HtmlBuilder implements StateRenderer<BeansState> {

    public static final String ACT_RELOAD = "Beans_ACT_RELOAD";

    @Override
    public HtmlElem render(BeansState state) {
        return simplePageWithTitle("Beans",
            form(
                div(text("%s beans loaded.".formatted(state.getCtx().getBeanDefinitionCount()))),
                div(inpSubmit(ACT_RELOAD, "Reload"))
            )
        );
    }
}
