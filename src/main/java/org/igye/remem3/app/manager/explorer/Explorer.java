package org.igye.remem3.app.manager.explorer;

public interface Explorer {
    ExplorerState getState();

    ExplorerState startCreatingNewDir(String stateId);

    ExplorerState cancelCreatingNewDir(String stateId);

    ExplorerState createNewDir(String stateId, String newDirName);

}
