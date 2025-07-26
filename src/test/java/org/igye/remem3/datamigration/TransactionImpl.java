package org.igye.remem3.datamigration;

import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.utils.BiConsumer;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Func;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransactionImpl implements Transaction {
    private static final Pattern PARAM_NAME_PATTERN = Pattern.compile("\\$[\\S]+");
    private final Connection connection;

    public TransactionImpl(Connection connection) {
        this.connection = connection;
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

}
