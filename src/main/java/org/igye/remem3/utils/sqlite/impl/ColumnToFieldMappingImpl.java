package org.igye.remem3.utils.sqlite.impl;

import lombok.SneakyThrows;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.sqlite.ColumnToFieldMapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ColumnToFieldMappingImpl implements ColumnToFieldMapping {
    private Map<String, String> colNameToFieldName = new HashMap<>();
    private Map<Class<?>, Map<String, Field>> fields = new HashMap<>();
    private Map<Class<?>, Constructor<?>> constructors = new HashMap<>();

    @Override
    public Field colNameToField(String colName, Class<?> clazz) {
        Map<String, Field> fieldsForClass = fields.get(clazz);
        if (fieldsForClass == null) {
            return save(colName, clazz);
        }
        Field field = fieldsForClass.get(colName);
        if (field == null) {
            return save(colName, clazz);
        }
        return field;
    }

    @Override
    public <T> Constructor<T> getConstructor(Class<T> clazz) {
        Constructor<?> constr = constructors.get(clazz);
        if (constr != null) {
            return (Constructor<T>) constr;
        }
        Constructor<?> argLessConstr = Arrays.stream(clazz.getDeclaredConstructors())
            .filter(c -> c.getGenericParameterTypes().length == 0)
            .findFirst()
            .orElseThrow(() ->
                new Exn(String.format("Cannot find a default constructor for %s", clazz.getCanonicalName()))
            );
        constructors.put(clazz, argLessConstr);
        return (Constructor<T>) argLessConstr;
    }

    @SneakyThrows
    private Field save(String colName, Class<?> clazz) {
        String fieldName = colNameToFieldName.computeIfAbsent(colName, this::colNameToFieldName);
        try {
            Field field = clazz.getDeclaredField(fieldName);
            fields.computeIfAbsent(clazz, _ -> new HashMap<>()).put(colName, field);
            return field;
        } catch (NoSuchFieldException e) {
            throw new Exn(String.format(
                "Class %s doesn't have the field '%s'.", clazz.getCanonicalName(), fieldName
            ));
        }
    }

    private String colNameToFieldName(String colName) {
        colName = colName.toLowerCase();
        StringBuilder sb = new StringBuilder();
        boolean prefix = true;
        boolean startOfWord = false;
        for (char ch : colName.toCharArray()) {
            if (ch == '_') {
                if (prefix) {
                    sb.append(ch);
                } else {
                    startOfWord = true;
                }
            } else {
                prefix = false;
                if (startOfWord) {
                    startOfWord = false;
                    sb.append(Character.toUpperCase(ch));
                } else {
                    sb.append(ch);
                }
            }
        }
        return sb.toString();
    }
}
