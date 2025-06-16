package org.igye.remem3.utils.sqlite.impl;

import lombok.SneakyThrows;
import org.igye.remem3.utils.sqlite.SqliteTransaction;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqliteTransactionImpl implements SqliteTransaction {
    private final Connection connection;

    public SqliteTransactionImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    @SneakyThrows
    public boolean execute(String command) {
        try (Statement statement = connection.createStatement()) {
            return statement.execute(command);
        }
    }

    @Override
    @SneakyThrows
    public List<Map<String, Object>> executeQuery(String query) {
        try (Statement statement = connection.createStatement()) {
            return extractData(statement.executeQuery(query));
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
