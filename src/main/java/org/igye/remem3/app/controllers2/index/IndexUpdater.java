package org.igye.remem3.app.controllers2.index;

import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.web.RequestParams;

public class IndexUpdater implements StateUpdater<IndexState> {
    @Override
    public IndexState update(IndexState state, RequestParams params) {
        return state;
    }

    @Override
    public Class<?> getSupportedType() {
        return IndexState.class;
    }
}
