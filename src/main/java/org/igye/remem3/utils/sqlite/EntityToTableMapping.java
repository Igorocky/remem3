package org.igye.remem3.utils.sqlite;

public interface EntityToTableMapping {
    Table getTableForEntity(Class<?> clazz);

    void registerTableForEntity(Class<?> clazz, Table table);
}
