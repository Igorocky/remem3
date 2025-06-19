package org.igye.remem3.utils.sqlite;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Database {
    <T> T transaction(Function<Transaction, T> consumer);

    void transactionV(Consumer<Transaction> consumer);

    boolean execute(String command);

    void insert(Table table, Object data);

    void insertMany(Table table, List<?> data);

    List<Map<String, Object>> select(String query, Map<String, Object> params);

    List<Map<String, Object>> select(String query);

    Object selectSingle(String query);

    <T> List<T> select(Class<T> clazz, String query, Map<String, Object> params);

    <T> List<T> select(Class<T> clazz, Table table);

    <T> List<T> select(Class<T> clazz, Table table, String where, List<String> orderBy, Map<String, Object> params);

    <T> List<T> selectById(Class<T> clazz, Table table, Collection<Long> ids);

    <T> T selectSingleById(Class<T> clazz, Table table, long id);

    <T> void update(Table table, T data);

    <T> void updateMany(Table table, List<T> data);

    void delete(Table table, Collection<Long> ids);

    void delete(Table table, long id);
}
