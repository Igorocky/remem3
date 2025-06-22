package org.igye.remem3.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.igye.remem3.app.db.entities.FolderEnt;
import org.igye.remem3.app.manager.explorer.Explorer;
import org.igye.remem3.app.manager.explorer.ExplorerState;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.utils.web.impl.RequestParamsImpl;
import org.igye.remem3.web.StatefulWebController;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ExplorerController extends HtmlBuilder
    implements StatefulWebController<ExplorerState, Supplier<ExplorerState>> {

    private static final String PAR_STATE_ID = "PAR_STATE_ID";

    private static final String ACT_START_CREATING_NEW_DIR = "ACT_START_CREATING_NEW_DIR";
    private static final String ACT_CANCEL_CREATING_NEW_DIR = "ACT_CANCEL_CREATING_NEW_DIR";
    private static final String ACT_CREATE_NEW_DIR = "ACT_CREATE_NEW_DIR";
    private static final String PAR_NEW_DIR_NAME = "PAR_NEW_DIR_NAME";

    private final Explorer explorer;

    public ExplorerController(Explorer explorer) {
        this.explorer = explorer;
    }

    @Override
    public String getPath() {
        return "explorer";
    }

    @Override
    public ExplorerState loadState(HttpServletRequest req) {
        return explorer.getState();
    }

    @Override
    public Optional<Supplier<ExplorerState>> decodeAction(HttpServletRequest req, ExplorerState state) {
        RequestParamsImpl params = new RequestParamsImpl(req);
        String stateId = params.getParam(PAR_STATE_ID);
        if (params.hasParam(ACT_START_CREATING_NEW_DIR)) {
            return Optional.of(() -> explorer.startCreatingNewDir(stateId));
        }
        if (params.hasParam(ACT_CANCEL_CREATING_NEW_DIR)) {
            return Optional.of(() -> explorer.cancelCreatingNewDir(stateId));
        }
        if (params.hasParam(ACT_CREATE_NEW_DIR)) {
            return Optional.of(() -> explorer.createNewDir(stateId, params.getParam(PAR_NEW_DIR_NAME)));
        }
        return Optional.empty();
    }

    @Override
    public synchronized ExplorerState updateState(ExplorerState state, Supplier<ExplorerState> action) {
        return action.get();
    }

    @Override
    public void saveState(ExplorerState state) {

    }

    @Override
    public String renderState(ExplorerState state) {
        return simplePageWithTitle("Explorer",
            form(
                inpHidden(PAR_STATE_ID, state.getId()),
                rndPath(state.getPath()),
                rndButtons(),
                state.isNewFolderDialog() ? rndNewDirDialog() : null,
                rndChildDirs(state.getChildDirs())
            )
        ).toString();
    }

    private HtmlElem rndChildDirs(List<FolderEnt> childDirs) {
        return uList(childDirs.stream().map(dir -> text(dir.name)).toList());
    }

    private HtmlElem rndNewDirDialog() {
        return table(List.of(List.of(
            inpText(PAR_NEW_DIR_NAME, "", ACT_CREATE_NEW_DIR),
            inpSubmit(ACT_CREATE_NEW_DIR, "Create"),
            inpSubmit(ACT_CANCEL_CREATING_NEW_DIR, "Cancel")
        )));
    }

    private HtmlElem rndButtons() {
        return table(List.of(List.of(
            inpSubmit(ACT_START_CREATING_NEW_DIR, "New folder")
        )));
    }

    private HtmlElem rndPath(List<FolderEnt> path) {
        return h6(
            path.stream().map(dir -> frag(text(" / "), text(dir.name))).toList()
        );
    }
}
