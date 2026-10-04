package org.igye.remem3.app.controllers.makenewdir;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.components.DirSelectorCmp;
import org.igye.remem3.app.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.web.RequestParams;

import java.io.File;
import java.util.List;

import static org.igye.remem3.app.controllers.makenewdir.MakeNewDirRenderer.ACT_CANCEL;
import static org.igye.remem3.app.controllers.makenewdir.MakeNewDirRenderer.ACT_CREATE;
import static org.igye.remem3.app.controllers.makenewdir.MakeNewDirRenderer.PAR_NEW_DIR_NAME;

@RequiredArgsConstructor
public class MakeNewDirUpdater implements StateUpdater<State> {
    private final Settings settings;
    private final Cache cache;
    private final Utils utils;

    @Override
    public Object update(State st, RequestParams params) {
        st = mergeStateFromParams(st, params);
        if (params.hasParam(ACT_CREATE)) {
            try {
                return st.getOnComplete().apply(actCreateNewDir(st));
            } catch (Exception ex) {
                st = st.withErrors(List.of(ex.getMessage()));
                return st;
            }
        } else if (params.hasParam(ACT_CANCEL)) {
            return st.getOnCancel();
        }
        return st;
    }

    private File actCreateNewDir(State st) {
        return utils.createNewDir(st.getParentDir().getSelectedDirectory(), st.getNewDirName());
    }

    private State mergeStateFromParams(State st, RequestParams params) {
        DirSelectorCmp parentDir = st.getParentDir();
        if (!parentDir.isReadonly()) {
            parentDir = ((DirSelectorCmpImpl) parentDir).setPath(params);
        }
        st = st
            .withErrors(List.of())
            .withParentDir(parentDir)
            .withNewDirName(params.getParam(PAR_NEW_DIR_NAME, ""));
        return st;
    }
}
