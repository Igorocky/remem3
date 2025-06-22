package org.igye.remem3.app.manager.explorer.impl;

import lombok.SneakyThrows;
import org.igye.remem3.app.db.entities.FolderEnt;
import org.igye.remem3.app.manager.explorer.Explorer;
import org.igye.remem3.app.manager.explorer.ExplorerState;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Func;
import org.igye.remem3.utils.sqlite.Database;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExplorerImpl implements Explorer {
    private final Database db;
    private ExplorerState state;
    private Map<Long, FolderEnt> allDirs;

    public ExplorerImpl(Database db) {
        this.db = db;
        loadStateFromDb();
    }

    @Override
    public ExplorerState getState() {
        return state;
    }

    @Override
    public ExplorerState startCreatingNewDir(String stateId) {
        checkStateId(stateId);
        updateState(st -> st.withNewFolderDialog(true));
        return getState();
    }

    @Override
    public ExplorerState cancelCreatingNewDir(String stateId) {
        checkStateId(stateId);
        updateState(st -> st.withNewFolderDialog(false));
        return getState();
    }

    @Override
    public ExplorerState createNewDir(String stateId, String newDirName) {
        checkStateId(stateId);
        FolderEnt newFolder = FolderEnt.builder().parentId(getCurrDirId()).name(newDirName).build();
        db.insert(newFolder);
        loadStateFromDb();
        return getState();
    }

    private void loadStateFromDb() {
        allDirs = db.select(FolderEnt.class).stream().collect(Collectors.toMap(dir -> dir.id, Function.identity()));
        Long currDirId = getCurrDirId();
        state = ExplorerState.builder()
            .path(getPathForDir(currDirId))
            .childDirs(getChildDirs(currDirId))
            .childCards(new ArrayList<>())
            .build();
    }

    private Long getCurrDirId() {
        return (state == null || state.getPath().isEmpty()) ? null : state.getPath().getLast().id;
    }

    private List<FolderEnt> getPathForDir(Long id) {
        List<FolderEnt> path = new ArrayList<>();
        while (id != null) {
            FolderEnt dir = allDirs.get(id);
            path.set(0, dir);
            id = dir.parentId;
        }
        return path;
    }

    private List<FolderEnt> getChildDirs(Long dirId) {
        return allDirs.values().stream()
            .filter(dir -> dirId == null && dir.parentId == null || dirId != null && dirId.equals(dir.parentId))
            .sorted(Comparator.comparing(dir -> dir.name))
            .toList();
    }

    @SneakyThrows
    private void updateState(Func<ExplorerState, ExplorerState> update) {
        state = update.apply(state).withId(UUID.randomUUID().toString());
    }

    private void checkStateId(String stateId) {
        if (!state.getId().equals(stateId)) {
            throw new Exn("!state.getId().equals(stateId)");
        }
    }
}
