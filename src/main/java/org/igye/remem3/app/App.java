package org.igye.remem3.app;

import org.igye.remem3.web.StatefulWebController;

import java.util.Optional;

public interface App {
    String getPropStr(String name);

    Optional<StatefulWebController> lookupController(String path);
}
