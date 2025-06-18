package org.igye.remem3.utils.sqlite;

import java.lang.reflect.Field;

public interface ColumnToFieldMapping {
    Field colNameToField(String colName, Class<?> clazz);
}
