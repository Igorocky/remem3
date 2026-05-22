package org.igye.remem3.app.controllers.makenewdir;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.components.DirSelectorCmp;
import org.igye.remem3.app.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.web.RequestParams;

import java.util.List;

import static org.igye.remem3.app.controllers.makenewdir.MakeNewDirRenderer.PAR_NEW_DIR_NAME;
import static org.igye.remem3.app.controllers.makenewdir.MakeNewDirRenderer.PAR_PARENT_DIR;

@RequiredArgsConstructor
public class MakeNewDirUpdater implements StateUpdater<State> {
    private final Settings settings;
    private final Cache cache;

    @Override
    public Object update(State st, RequestParams params) {
        st = mergeStateFromParams(st, params);
        return st;
    }

    private State mergeStateFromParams(State st, RequestParams params) {
        DirSelectorCmp parentDir = st.getParentDir();
        st = st
            .withErrors(List.of())
            .withParentDir(
                parentDir.isReadonly()
                    ? parentDir
                    : new DirSelectorCmpImpl(settings, cache, PAR_PARENT_DIR, false, params)
            )
            .withNewDirName(params.getParam(PAR_NEW_DIR_NAME, ""));
        return st;
    }
}
