package org.igye.remem3.app.state;

import org.igye.remem3.web.RequestParams;

public interface StateRepository {
    String getActualStateId(String stateId);

    void updateState(String stateId, RequestParams params);

    String rednerState(String stateId);
}
