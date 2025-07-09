package org.igye.remem3.web;

import java.util.Optional;

public interface StatefulWebController<S, A> {
    String getPath();

    S loadState(RequestParams params);

    Optional<A> decodeAction(RequestParams params, S state);

    S updateState(S state, A action);

    void saveState(S state);

    String renderState(S state);

    void setContextPath(String contextPath);
}
