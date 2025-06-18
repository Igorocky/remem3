package org.igye.remem3.utils.sqlite;

import java.util.List;
import java.util.Map;

public interface Transaction {
    boolean execute(String command);

    List<Map<String, Object>> executeQuery(String query);

    Object selectSingle(String query);

    void insert(Table table, Object data);

    void insertMany(Table table, List<?> data);

    <T> List<T> selectAll(Table table, Class<T> clazz, List<String> columnsToOrderBy);

    <T> List<T> selectAll(Table table, Class<T> clazz);
}
