package org.igye.remem3.controllers;

import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.web.RequestParams;
import org.igye.remem3.web.StatefulWebController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class IndexController extends HtmlBuilder implements StatefulWebController<Void, Void> {
    private final List<String> paths;

    public IndexController(List<String> paths) {
        this.paths = paths;
    }

    @Override
    public String getPath() {
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
            "Example Web App Index",
            paths.stream()
                .map(path -> frag(
                    br(),
                    h("a", Map.of("href", path), text(path))
                ))
                .toList()
        ).toString();
    }
}
