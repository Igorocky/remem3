package org.igye.remem3.datamigration;

import java.util.List;
import java.util.Map;

public interface Transaction {
    boolean execute(String command);

    List<Map<String, Object>> select(String query, Map<String, Object> params);

    List<Map<String, Object>> select(String query);

    Object selectSingle(String query);
}
