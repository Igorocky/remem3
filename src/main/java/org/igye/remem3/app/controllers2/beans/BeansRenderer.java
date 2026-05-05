package org.igye.remem3.app.controllers2.beans;

import org.igye.remem3.app.state.StateRenderer;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.List;

public class BeansRenderer extends HtmlBuilder implements StateRenderer<BeansState> {

    public static final String ACT_RELOAD = "Beans_ACT_RELOAD";

    @Override
    public HtmlElem render(BeansState st) {
        return simplePageWithTitle("Beans",
            form(
                div(text("%s beans loaded.".formatted(st.getCtx().getBeanDefinitionCount()))),
                div(inpSubmit(ACT_RELOAD, "Reload")),
                div(rndAllBeans(st.getCtx()))
            )
        );
    }

    private HtmlElem rndAllBeans(ApplicationContext ctx) {
        return table(
            Arrays.stream(ctx.getBeanDefinitionNames())
                .map(name -> List.of(text(name), text(ctx.getBean(name))))
                .toList()
        );
    }
}
