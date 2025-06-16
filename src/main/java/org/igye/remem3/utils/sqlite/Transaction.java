package org.igye.remem3.utils.sqlite;

import java.util.List;
import java.util.Map;

public interface Transaction {
    boolean execute(String command);

    List<Map<String, Object>> executeQuery(String query);

    Object selectSingle(String query);
}
