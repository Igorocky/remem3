package org.igye.remem3.web;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.igye.remem3.app.App;
import org.igye.remem3.app.impl.AppImpl;
import org.igye.remem3.utils.RememExn;

import java.util.Optional;

public class ApiDispatcherServlet extends HttpServlet {
    private App app;

    public ApiDispatcherServlet() {
        app = AppImpl.getInstance();
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        process(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        process(req, resp);
    }

    @SneakyThrows
    private void process(HttpServletRequest req, HttpServletResponse resp) {
        String path = Optional.ofNullable(req.getPathInfo()).map(s -> s.substring(1)).orElse("");
        StatefulWebController controller = app.lookupController(path)
            .orElseThrow(() -> new RememExn(String.format(
                "Cannot find a controller for the path '%s'.", path
            )));
        Object state = controller.loadState(req);
        Object newState = ((Optional<Object>) controller.decodeAction(req, state))
            .map(act -> controller.updateState(state, act))
            .orElse(state);
        controller.saveState(newState);
        resp.getWriter().write(controller.renderState(newState));
    }
}