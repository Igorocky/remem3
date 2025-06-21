package org.igye.remem3.web;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public interface StatefulWebController<S, A> {
    String getPath();

    S loadState(HttpServletRequest req);

    Optional<A> decodeAction(HttpServletRequest req, S state);

    S updateState(S state, A action);

    void saveState(S state);

    String renderState(S state);

    void setContextPath(String contextPath);
}
