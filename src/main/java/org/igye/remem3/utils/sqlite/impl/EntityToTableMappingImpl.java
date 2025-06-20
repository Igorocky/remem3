package org.igye.remem3.utils.sqlite.impl;

import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.sqlite.EntityToTableMapping;
import org.igye.remem3.utils.sqlite.Table;

import java.util.HashMap;
import java.util.Map;

public class EntityToTableMappingImpl implements EntityToTableMapping {
    private final Map<Class<?>, Table> entToTblMap = new HashMap<>();

    @Override
    public Table getTableForEntity(Class<?> clazz) {
        Table table = entToTblMap.get(clazz);
        if (table == null) {
            throw new Exn(String.format("No table registered for the entity %s", clazz.getCanonicalName()));
        }
        return table;
    }

    @Override
    public void registerTableForEntity(Class<?> clazz, Table table) {
        Table existingTable = entToTblMap.get(clazz);
        if (existingTable != null) {
            throw new Exn(String.format(
                "Cannot register multiple tables for the same entity %s, the previously registered table is %s",
                clazz.getCanonicalName(),
                table.getClass().getCanonicalName()
            ));
        }
        entToTblMap.put(clazz, table);
    }
}
