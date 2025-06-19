package org.igye.remem3.utils.sqlite;

import java.util.List;
import java.util.Map;

public interface Transaction {
    boolean execute(String command);

    List<Map<String, Object>> executeQuery(String query);

    Object selectSingle(String query);

    void insert(Table table, Object data);

    void insertMany(Table table, List<?> data);

    <T> List<T> select(Class<T> clazz, String query, Map<String, Object> params);

    <T> List<T> select(Class<T> clazz, Table table);

    <T> List<T> select(Class<T> clazz, Table table, String where, List<String> orderBy, Map<String, Object> params);
}
