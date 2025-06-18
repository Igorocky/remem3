package org.igye.remem3.utils.sqlite.impl;

import lombok.SneakyThrows;
import org.igye.remem3.utils.RememExn;
import org.igye.remem3.utils.sqlite.Column;
import org.igye.remem3.utils.sqlite.Table;
import org.igye.remem3.utils.sqlite.Transaction;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SqliteTransaction implements Transaction {
    private final Connection connection;

    public SqliteTransaction(Connection connection) {
        this.connection = connection;
    }

    @Override
    @SneakyThrows
    public boolean execute(String command) {
        try (Statement statement = connection.createStatement()) {
            return statement.execute(command);
        } catch (Exception ex) {
            throw new RememExn(String.format("SQL command failed:\n%s", command), ex);
        }
    }

    @Override
    @SneakyThrows
    public List<Map<String, Object>> executeQuery(String query) {
        try (Statement statement = connection.createStatement()) {
            return extractData(statement.executeQuery(query));
        } catch (Exception ex) {
            throw new RememExn(String.format("SQL query failed:\n%s", query), ex);
        }
    }

    @SneakyThrows
    @Override
    public Object selectSingle(String query) {
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(query);
            if (!resultSet.next()) {
                throw new RememExn("Expected exactly one row, but got 0.");
            }
            int columnCount = resultSet.getMetaData().getColumnCount();
            if (columnCount != 1) {
                throw new RememExn(String.format("Expected exactly one column, but got %s.", columnCount));
            }
            Object res = resultSet.getObject(1);
            if (resultSet.next()) {
                throw new RememExn("Expected exactly one row, but got at least two.");
            }
            return res;
        } catch (Exception ex) {
            throw new RememExn(String.format("SQL query failed:\n%s", query), ex);
        }
    }

    @SneakyThrows
    @Override
    public void insert(Table table, Object data) {
        List<String> colNames = table.getColumns().stream()
            .map(Column::getName)
            .toList();
        Class<?> dataClass = data.getClass();
        List<Field> fields = colNames.stream()
            .map(this::colNameToFieldName)
            .map(fieldName -> {
                try {
                    return dataClass.getDeclaredField(fieldName);
                } catch (NoSuchFieldException ex) {
                    throw new RememExn(ex);
                }
            })
            .toList();
        String placeholders = Stream.iterate("?", _ -> "?")
            .limit(colNames.size())
            .collect(Collectors.joining(","));
        String query = String.format(
            "insert into %s(%s) values(%s) returning %s",
            table.getName(), colNames, placeholders, table.getIdColumnName()
        );
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            for (int i = 0; i < fields.size(); i++) {
                stmt.setObject(i + 1, fields.get(i).get(data));
            }
            ResultSet rs = stmt.executeQuery();
            Field idField = dataClass.getDeclaredField(table.getIdColumnName());
            if (rs.next()) {
                idField.set(data, rs.getLong(1));
            } else {
                throw new RememExn("No generated key was returned.");
            }
        }
    }

    @SneakyThrows
    @Override
    public void insert(Table table, List<Object> data) {
        data.forEach(elem -> insert(table, elem));
    }

    private String colNameToFieldName(String colName) {
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

    @SneakyThrows
    private List<Map<String, Object>> extractData(ResultSet resultSet) {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        List<String> colNames = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            colNames.add(metaData.getColumnName(i));
        }
        ArrayList<Map<String, Object>> res = new ArrayList<>();
        while (resultSet.next()) {
            HashMap<String, Object> row = new HashMap<>();
            res.add(row);
            for (String colName : colNames) {
                row.put(colName, resultSet.getObject(colName));
            }
        }
        return res;
    }
}
