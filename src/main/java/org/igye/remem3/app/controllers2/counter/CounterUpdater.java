package org.igye.remem3.app.controllers2.counter;

import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.web.RequestParams;

import static org.igye.remem3.app.controllers2.counter.CounterRenderer.ACT_INC;

public class CounterUpdater implements StateUpdater<CounterState> {
    @Override
    public CounterState update(CounterState state, RequestParams params) {
        if (params.hasParam(ACT_INC)) {
            state.setCount(state.getCount() + 1);
        }
        return state;
    }

}
