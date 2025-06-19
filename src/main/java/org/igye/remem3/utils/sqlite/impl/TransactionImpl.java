package org.igye.remem3.utils.sqlite.impl;

import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.utils.BiConsumer;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Func;
import org.igye.remem3.utils.sqlite.Column;
import org.igye.remem3.utils.sqlite.ColumnToFieldMapping;
import org.igye.remem3.utils.sqlite.Table;
import org.igye.remem3.utils.sqlite.Transaction;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TransactionImpl implements Transaction {
    private static final Pattern PARAM_NAME_PATTERN = Pattern.compile("\\$[\\S]+");
    private static final int IN_BATCH_SIZE = 1000;
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
        } catch (SQLException ex) {
            throw new Exn(String.format("SQL command failed:\n%s", command), ex);
        }
    }

    @SneakyThrows
    @Override
    public void insert(Table table, Object data) {
        List<String> colNamesToInsert = table.getColumns().stream()
            .filter(col -> getColValue(col.getName(), data) != null)
            .map(Column::getName)
            .toList();
        List<String> colNamesToAutoGenerate = new ArrayList<>();
        colNamesToAutoGenerate.add(table.getIdColumnName());
        table.getColumns().stream()
            .filter(col -> !colNamesToInsert.contains(col.getName()) && StringUtils.isNotBlank(col.getDefaultValue()))
            .map(Column::getName)
            .forEach(colNamesToAutoGenerate::add);
        String placeholders = Stream.iterate("?", _ -> "?")
            .limit(colNamesToInsert.size())
            .collect(Collectors.joining(","));
        String query = String.format(
            "insert into %s(%s) values(%s) returning %s",
            table.getName(),
            StringUtils.join(colNamesToInsert, ","),
            placeholders,
            StringUtils.join(colNamesToAutoGenerate, ",")
        );
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            for (int i = 0; i < colNamesToInsert.size(); i++) {
                stmt.setObject(i + 1, getColValue(colNamesToInsert.get(i), data));
            }
            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                for (int i = 0; i < colNamesToAutoGenerate.size(); i++) {
                    setColValue(colNamesToAutoGenerate.get(i), data, resultSet.getObject(i + 1));
                }
            } else {
                throw new Exn("No generated key was returned.");
            }
        } catch (SQLException ex) {
            throw new Exn(String.format("SQL query failed:\n%s", query), ex);
        }
    }

    @SneakyThrows
    @Override
    public void insertMany(Table table, List<?> data) {
        data.forEach(elem -> insert(table, elem));
    }

    @SneakyThrows
    @Override
    public List<Map<String, Object>> select(String query, Map<String, Object> params) {
        return prepareStatement(query, params, stmt -> {
            try (ResultSet rs = stmt.executeQuery()) {
                return collectData(rs);
            }
        });
    }

    @Override
    public List<Map<String, Object>> select(String query) {
        return select(query, null);
    }

    @SneakyThrows
    @Override
    public Object selectSingle(String query) {
        try (
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query)
        ) {
            if (!resultSet.next()) {
                throw new Exn("Expected exactly one row, but got 0.");
            }
            int columnCount = resultSet.getMetaData().getColumnCount();
            if (columnCount != 1) {
                throw new Exn(String.format("Expected exactly one column, but got %s.", columnCount));
            }
            Object res = resultSet.getObject(1);
            if (resultSet.next()) {
                throw new Exn("Expected exactly one row, but got at least two.");
            }
            return res;
        } catch (SQLException ex) {
            throw new Exn(String.format("SQL query failed:\n%s", query), ex);
        }
    }

    @SneakyThrows
    @Override
    public <T> List<T> select(Class<T> clazz, String query, Map<String, Object> params) {
        return prepareStatement(query, params, stmt -> {
            try (ResultSet rs = stmt.executeQuery()) {
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
                    T rowObj = makeNewInstance(clazz);
                    res.add(rowObj);
                    for (int i = 0; i < colNames.size(); i++) {
                        setColValue(colNames.get(i), rowObj, rs.getObject(i + 1));
                    }
                }
                return res;
            }
        });
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

    @Override
    public <T> List<T> selectById(Class<T> clazz, Table table, Collection<Long> ids) {
        List<List<Long>> parts = ListUtils.partition(new ArrayList<>(ids), IN_BATCH_SIZE);
        String idsCondition = parts.stream()
            .map(part -> part.stream().map(_ -> "?").collect(Collectors.joining(",")))
            .map(placeholders -> String.format("%s in (%s)", table.getIdColumnName(), placeholders))
            .collect(Collectors.joining(" or "));
        String query = String.format(
            "select %s from %s where %s", table.getPrefixedColumns(""), table.getName(), idsCondition
        );
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            for (int p = 0; p < parts.size(); p++) {
                List<Long> part = parts.get(p);
                int partFirstIdx = p * IN_BATCH_SIZE + 1;
                for (int i = 0; i < part.size(); i++) {
                    Long id = part.get(i);
                    stmt.setLong(partFirstIdx + i, id);
                }
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return collectData(rs, clazz);
            }
        } catch (SQLException ex) {
            throw new Exn(String.format("SQL query failed:\n%s", query), ex);
        }
    }

    @Override
    public <T> T selectSingleById(Class<T> clazz, Table table, long id) {
        List<T> res = selectById(clazz, table, Collections.singletonList(id));
        if (res.size() != 1) {
            throw new Exn("Expected exactly 1 row but got " + res.size() + ".");
        }
        return res.getFirst();
    }

    @Override
    public <T> void update(Table table, T data) {
        updateMany(table, Collections.singletonList(data));
    }

    @SneakyThrows
    @Override
    public <T> void updateMany(Table table, List<T> data) {
        if (CollectionUtils.isEmpty(data)) {
            return;
        }
        List<String> colNames = table.getColumns().stream().map(Column::getName).toList();
        String listOfCols = colNames.stream().map(colName -> colName + " = ?").collect(Collectors.joining(", "));
        String query = String.format(
            "update %s set %s where %s = ?", table.getName(), listOfCols, table.getIdColumnName()
        );
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            for (T obj : data) {
                for (int i = 0; i < colNames.size(); i++) {
                    Object value = getColValue(colNames.get(i), obj);
                    stmt.setObject(i + 1, value);
                }
                Object idValue = getColValue(table.getIdColumnName(), obj);
                stmt.setObject(colNames.size() + 1, idValue);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
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

    private List<Map<String, Object>> collectData(ResultSet resultSet) {
        List<Map<String, Object>> res = new ArrayList<>();
        collectData(resultSet, (rs, colNames) -> {
            HashMap<String, Object> row = new HashMap<>();
            res.add(row);
            for (int i = 0; i < colNames.size(); i++) {
                row.put(colNames.get(i), rs.getObject(i + 1));
            }
        });
        return res;
    }

    private <T> List<T> collectData(ResultSet resultSet, Class<T> clazz) {
        List<T> res = new ArrayList<>();
        collectData(resultSet, (rs, colNames) -> {
            T rowObj = makeNewInstance(clazz);
            res.add(rowObj);
            for (int i = 0; i < colNames.size(); i++) {
                setColValue(colNames.get(i), rowObj, rs.getObject(i + 1));
            }
        });
        return res;
    }

    @SneakyThrows
    private void collectData(ResultSet resultSet, BiConsumer<ResultSet, List<String>> rowConsumer) {
        List<String> colNames = null;
        while (resultSet.next()) {
            if (colNames == null) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                colNames = new ArrayList<>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    colNames.add(metaData.getColumnName(i));
                }
            }
            rowConsumer.consume(resultSet, colNames);
        }
    }

    @SneakyThrows
    private <T> T prepareStatement(String query, Map<String, Object> params, Func<PreparedStatement, T> consumer) {
        Pair<String, List<Object>> queryWithPlaceholdersAndParamValues = addQuestionMarkPlaceholders(query, params);
        String queryWithPlaceholders = queryWithPlaceholdersAndParamValues.getLeft();
        List<Object> paramValues = queryWithPlaceholdersAndParamValues.getRight();
        try (PreparedStatement stmt = connection.prepareStatement(queryWithPlaceholders)) {
            for (int i = 0; i < paramValues.size(); i++) {
                stmt.setObject(i + 1, paramValues.get(i));
            }
            return consumer.apply(stmt);
        } catch (SQLException ex) {
            throw new Exn(String.format("SQL query failed:\n%s", queryWithPlaceholders), ex);
        }
    }

    private Field getFieldByColName(String colName, Object obj) {
        return colToFieldMapping.colNameToField(colName, obj.getClass());
    }

    @SneakyThrows
    private Object getColValue(String colName, Object obj) {
        return getFieldByColName(colName, obj).get(obj);
    }

    @SneakyThrows
    private void setColValue(String colName, Object obj, Object value) {
        Field field = getFieldByColName(colName, obj);
        if (value == null) {
            field.set(obj, null);
        } else {
            Class<?> valueClass = value.getClass();
            Class<?> fieldClass = field.getType();
            if (fieldClass.isAssignableFrom(valueClass)) {
                field.set(obj, value);
            } else if (fieldClass.equals(Long.class) && value instanceof Integer intVal) {
                field.set(obj, Long.valueOf(intVal.longValue()));
            } else {
                throw new Exn(String.format(
                    "Cannot set %s.%s=%s", fieldClass.getCanonicalName(), field.getName(), value
                ));
            }
        }
    }

    @SneakyThrows
    private <T> T makeNewInstance(Class<T> clazz) {
        return colToFieldMapping.getConstructor(clazz).newInstance();
    }
}
