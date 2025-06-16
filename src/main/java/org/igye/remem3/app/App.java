package org.igye.remem3.app;

import org.igye.remem3.utils.sqlite.SqliteRepo;
import org.igye.remem3.web.StatefulWebController;

import java.util.List;
import java.util.Optional;

public interface App {
    String getPropStr(String propName);

    String getPropStr(String propName, String defaultValue);

    Long getPropLong(String propName);

    Long getPropLong(String propName, Long defaultValue);

    Integer getPropInt(String propName);

    Integer getPropInt(String propName, Integer defaultValue);

    List<String> getPropList(String propName);

    List<String> getPropList(String propName, List<String> defaultValue);

    Optional<StatefulWebController> lookupController(String path);

    SqliteRepo getSqliteRepo();
}
