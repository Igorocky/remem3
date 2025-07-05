package org.igye.remem3.app;

import org.igye.remem3.utils.Utils;
import org.igye.remem3.web.StatefulWebController;

import java.util.List;
import java.util.Optional;

public interface App {
    void reloadProperties();

    String getPropStr(String propName, String defaultValue);

    String getPropStr(String propName);

    Long getPropLong(String propName, Long defaultValue);

    Long getPropLong(String propName);

    Integer getPropInt(String propName, Integer defaultValue);

    Integer getPropInt(String propName);

    List<String> getPropList(String propName, List<String> defaultValue);

    List<String> getPropList(String propName);

    Optional<StatefulWebController> lookupController(String path);

    Utils getUtils();
}
