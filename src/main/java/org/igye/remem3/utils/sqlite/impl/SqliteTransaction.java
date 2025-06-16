package org.igye.remem3.utils.sqlite.impl;

import lombok.SneakyThrows;
import org.igye.remem3.utils.RememExn;
import org.igye.remem3.utils.sqlite.Transaction;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
