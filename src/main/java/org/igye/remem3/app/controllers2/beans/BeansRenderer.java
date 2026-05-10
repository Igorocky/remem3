package org.igye.remem3.app.controllers2.beans;

import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.state.StateRenderer;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListenerFactory;

import java.util.Arrays;
import java.util.List;

public class BeansRenderer extends HtmlBuilder implements StateRenderer<BeansState> {

    public static final String ACT_RELOAD = "Beans_ACT_RELOAD";

    @Override
    public HtmlElem render(BeansState st) {
        ApplicationContext ctx = st.getCtx();
        List<Pair<String, Object>> filteredBeans = Arrays.stream(ctx.getBeanDefinitionNames())
            .map(name -> Pair.of(name, ctx.getBean(name)))
            .filter(pair -> !(
                pair.getLeft().startsWith("__")
                    || pair.getLeft().equals("conversionService")
                    || pair.getRight() instanceof BeanFactoryPostProcessor
                    || pair.getRight() instanceof BeanPostProcessor
                    || pair.getRight() instanceof EventListenerFactory
            ))
            .toList();
        return simplePageWithTitle("Beans",
            form(
                div(text("%s beans loaded.".formatted(filteredBeans.size()))),
                div(inpSubmit(ACT_RELOAD, "Reload")),
                div(rndAllBeans(filteredBeans))
            )
        );
    }

    private HtmlElem rndAllBeans(List<Pair<String, Object>> beans) {
        return table(
            beans.stream()
                .map(pair -> List.of(text(pair.getLeft()), text(pair.getRight())))
                .toList()
        ).attr("class", "table-single-border");
    }
}
