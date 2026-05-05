package org.igye.remem3.app.controllers2.beans;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.web.RequestParams;

import static org.igye.remem3.app.controllers2.beans.BeansRenderer.ACT_RELOAD;

@RequiredArgsConstructor
public class BeansUpdater implements StateUpdater<BeansState> {
    private final BeansConstructor beansConstructor;

    @Override
    public BeansState update(BeansState state, RequestParams params) {
        if (params.hasParam(ACT_RELOAD)) {
            return beansConstructor.construct();
        }
        return state;
    }
}
