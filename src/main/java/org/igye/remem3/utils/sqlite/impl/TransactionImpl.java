package org.igye.remem3.utils.sqlite.impl;

import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.utils.RememExn;
import org.igye.remem3.utils.sqlite.Column;
import org.igye.remem3.utils.sqlite.ColumnToFieldMapping;
import org.igye.remem3.utils.sqlite.Table;
import org.igye.remem3.utils.sqlite.Transaction;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TransactionImpl implements Transaction {
    private static final Pattern PARAM_NAME_PATTERN = Pattern.compile("\\$[\\S]+");
    private final Connection connection;
    private final ColumnToFieldMapping colToFieldMapping;

    public TransactionImpl(Connection connection, ColumnToFieldMapping colToFieldMapping) {
        this.connection = connection;
        this.colToFieldMapping = colToFieldMapping;
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
        String placeholders = Stream.iterate("?", _ -> "?")
            .limit(colNames.size())
            .collect(Collectors.joining(","));
        String query = String.format(
            "insert into %s(%s) values(%s) returning %s",
            table.getName(), colNames, placeholders, table.getIdColumnName()
        );
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            for (int i = 0; i < colNames.size(); i++) {
                Object value = colToFieldMapping.colNameToField(colNames.get(i), dataClass).get(data);
                stmt.setObject(i + 1, value);
            }
            ResultSet rs = stmt.executeQuery();
            Field idField = colToFieldMapping.colNameToField(table.getIdColumnName(), dataClass);
            if (rs.next()) {
                idField.set(data, rs.getLong(1));
            } else {
                throw new RememExn("No generated key was returned.");
            }
        }
    }

    @SneakyThrows
    @Override
    public void insertMany(Table table, List<?> data) {
        data.forEach(elem -> insert(table, elem));
    }

    @SneakyThrows
    @Override
    public <T> List<T> select(Class<T> clazz, String query, Map<String, Object> params) {
        Pair<String, List<Object>> queryWithPlaceholdersAndParamValues = addQuestionMarkPlaceholders(query, params);
        String queryWithPlaceholders = queryWithPlaceholdersAndParamValues.getLeft();
        List<Object> paramValues = queryWithPlaceholdersAndParamValues.getRight();
        try (PreparedStatement stmt = connection.prepareStatement(queryWithPlaceholders)) {
            for (int i = 0; i < paramValues.size(); i++) {
                stmt.setObject(i + 1, paramValues.get(i));
            }
            ResultSet rs = stmt.executeQuery();
            ArrayList<T> res = new ArrayList<>();
            List<String> colNames = null;
            while (rs.next()) {
                if (colNames == null) {
                    colNames = new ArrayList<>();
                    int colNum = rs.getMetaData().getColumnCount();
                    for (int i = 0; i < colNum; i++) {
                        colNames.add(rs.getMetaData().getColumnName(i + 1));
                    }
                }
                T rowObj = colToFieldMapping.getConstructor(clazz).newInstance();
                res.add(rowObj);
                for (int i = 0; i < colNames.size(); i++) {
                    colToFieldMapping.colNameToField(colNames.get(i), clazz).set(rowObj, rs.getObject(i + 1));
                }
            }
            return res;
        }
    }

    @Override
    public <T> List<T> select(Class<T> clazz, Table table) {
        return select(clazz, String.format("select %s from %s", table.getPrefixedColumns(""), table.getName()), null);
    }

    @Override
    public <T> List<T> select(
        Class<T> clazz,
        Table table,
        String where,
        List<String> orderBy,
        Map<String, Object> params
    ) {
        StringBuilder sb = new StringBuilder(String.format(
            "select %s from %s", table.getPrefixedColumns(""), table.getName()
        ));
        if (StringUtils.isNotBlank(where)) {
            sb.append(" where ").append(where);
        }
        if (CollectionUtils.isNotEmpty(orderBy)) {
            sb.append(" order by ").append(StringUtils.join(orderBy, ", "));
        }
        return select(clazz, sb.toString(), params);
    }

    private Pair<String, List<Object>> addQuestionMarkPlaceholders(String query, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return Pair.of(query, Collections.emptyList());
        }
        StringBuilder newQuery = new StringBuilder();
        List<Object> paramValues = new ArrayList<>();
        Matcher matcher = PARAM_NAME_PATTERN.matcher(query);
        int lastEnd = 0;
        while (matcher.find()) {
            newQuery.append(query.substring(lastEnd, matcher.start())).append("?");
            paramValues.add(params.get(matcher.group()));
            lastEnd = matcher.end();
        }
        return Pair.of(newQuery.toString(), paramValues);
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
