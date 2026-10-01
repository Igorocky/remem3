package org.igye.remem3.app.controllers.beans;

import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.state.StateRenderer;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;

import java.util.List;

public class BeansRenderer extends HtmlBuilder implements StateRenderer<BeansState> {

    public static final String ACT_RELOAD = "Beans_ACT_RELOAD";

    @Override
    public HtmlElem render(BeansState st) {
        List<Pair<String, Object>> filteredBeans = st.getBeans();
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
