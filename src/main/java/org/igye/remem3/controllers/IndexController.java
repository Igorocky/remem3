package org.igye.remem3.controllers;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.web.RequestParams;
import org.igye.remem3.web.StatefulWebController;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IndexController extends HtmlBuilder implements StatefulWebController<Void, Void> {
    private final List<StatefulWebController> controllers;

    @Override
    public String getId() {
        return "";
    }

    @Override
    public Void loadState(RequestParams params) {
        return null;
    }

    @Override
    public Optional<Void> decodeAction(RequestParams params, Void state) {
        return Optional.empty();
    }

    @Override
    public Void updateState(Void state, Void action) {
        return null;
    }

    @Override
    public void saveState(Void state) {

    }

    @Override
    public String renderState(Void state) {
        return simplePageWithTitle(
            "ReMem Index",
            controllers.stream()
                .map(controller -> frag(
                    br(),
                    h("a", Map.of("href", controller.getId()), text(controller.getTitle()))
                ))
                .toList()
        ).toString();
    }
}
