package org.igye.remem3.app.state;

import org.igye.remem3.web.RequestParams;

public interface StateUpdater<T, U> extends TypeSupporter {
    U update(T state, RequestParams params);
}
