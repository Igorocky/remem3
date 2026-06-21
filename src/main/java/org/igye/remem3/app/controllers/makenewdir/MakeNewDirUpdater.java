package org.igye.remem3.app.controllers.makenewdir;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.components.DirSelectorCmp;
import org.igye.remem3.app.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.web.RequestParams;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.igye.remem3.app.controllers.makenewdir.MakeNewDirRenderer.ACT_CANCEL;
import static org.igye.remem3.app.controllers.makenewdir.MakeNewDirRenderer.ACT_CREATE;
import static org.igye.remem3.app.controllers.makenewdir.MakeNewDirRenderer.PAR_NEW_DIR_NAME;

@RequiredArgsConstructor
public class MakeNewDirUpdater implements StateUpdater<State> {
    private final Settings settings;
    private final Cache cache;

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
        File parentDir = st.getParentDir().getSelectedDirectory();
        if (!parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new Exn("Cannot create the parent directory '%s'.".formatted(parentDir));
            }
        }
        String newDirName = st.getNewDirName().trim()
            .replaceAll("\\s+", " ")
            .replaceAll("[^a-zA-Z0-9-_.\\s]", "");
        if (StringUtils.isBlank(newDirName)) {
            throw new Exn("Directory name cannot be blank.");
        }
        if (".".equals(newDirName)) {
            throw new Exn("Directory name cannot be '.'");
        }
        if ("..".equals(newDirName)) {
            throw new Exn("Directory name cannot be '..'");
        }
        String[] existingDirs = parentDir.list();
        if ((existingDirs == null ? List.of() : Arrays.asList(existingDirs)).contains(newDirName)) {
            throw new Exn("A directory with name '%s' already exists.".formatted(newDirName));
        }
        File newDir = new File(parentDir, newDirName);
        if (!newDir.mkdirs()) {
            throw new Exn("Cannot create a new directory '%s'.".formatted(newDirName));
        }
        return newDir;
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
