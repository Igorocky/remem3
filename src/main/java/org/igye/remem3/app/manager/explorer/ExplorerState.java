package org.igye.remem3.app.manager.explorer;

import lombok.Builder;
import lombok.Getter;
import lombok.With;
import org.igye.remem3.app.db.entities.FolderEnt;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
@With
@Getter
public class ExplorerState {
    @Builder.Default
    private String id = UUID.randomUUID().toString();
    private List<FolderEnt> path;
    private List<FolderEnt> childDirs;
    private List<Object> childCards;
    private boolean newFolderDialog;
    @Builder.Default
    private List<Object> cutItems = new ArrayList<>();
    @Builder.Default
    private List<Object> copiedItems = new ArrayList<>();
}
