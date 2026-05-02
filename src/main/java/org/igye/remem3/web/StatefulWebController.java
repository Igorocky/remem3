package org.igye.remem3.web;

import java.util.Optional;

public interface StatefulWebController<S, A> {
    String getId();

    default String getTitle() {
        return getId();
    }

    void setContextPath(String contextPath);

    S loadState(RequestParams params);

    Optional<A> decodeAction(RequestParams params, S state);

    S updateState(S state, A action);

    void saveState(S state);

    String renderState(S state);
}
