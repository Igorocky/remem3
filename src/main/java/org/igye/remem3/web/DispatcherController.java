package org.igye.remem3.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.web.impl.RequestParamsImpl;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class DispatcherController {
    private final Map<String, StatefulWebController<?, ?>> controllers;

    @RequestMapping("/{controllerId}")
    public String process(HttpServletRequest req, @PathVariable String controllerId) {
        StatefulWebController controller = controllers.get(controllerId);
        if (controller == null) {
            throw new Exn(String.format("Cannot find a controller for the id '%s'.", controllerId));
        }
        RequestParams params = new RequestParamsImpl(req);
        Object state = controller.loadState(params);
        Optional<Object> action = controller.decodeAction(params, state);
        Object newState = action.map(act -> controller.updateState(state, act)).orElse(state);
        controller.saveState(newState);
        return "<!DOCTYPE html>\n" + controller.renderState(newState);
    }

    @RequestMapping("/")
    public String processIndex(HttpServletRequest req) {
        return process(req, "");
    }
}
