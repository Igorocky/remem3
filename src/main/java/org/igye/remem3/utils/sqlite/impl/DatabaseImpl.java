package org.igye.remem3.utils.sqlite.impl;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp2.BasicDataSource;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.sqlite.ColumnToFieldMapping;
import org.igye.remem3.utils.sqlite.Database;
import org.igye.remem3.utils.sqlite.Table;
import org.igye.remem3.utils.sqlite.Transaction;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class DatabaseImpl implements Database {
    private final BasicDataSource dataSource;
    private final ColumnToFieldMapping columnToFieldMapping;

    public static DatabaseImpl getInMemoryDb() {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("org.sqlite.JDBC");
        ds.setUrl("jdbc:sqlite::memory:");
        return new DatabaseImpl(ds);
    }

    public DatabaseImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
        this.columnToFieldMapping = new ColumnToFieldMappingImpl();
    }

    @SneakyThrows
    @Override
    public <T> T transaction(Function<Transaction, T> consumer) {
        try (Connection connection = dataSource.getConnection()) {
            Transaction tx = new TransactionImpl(connection, columnToFieldMapping);
            boolean commit = false;
            try {
                enableForeignKeys(tx);
                tx.execute("BEGIN");
                T result = consumer.apply(tx);
                commit = true;
                return result;
            } finally {
                if (commit) {
                    commit(tx);
                } else {
                    rollback(tx);
                }
            }
        }
    }

    @Override
    public void transactionV(Consumer<Transaction> consumer) {
        transaction(tx -> {
            consumer.accept(tx);
            return null;
        });
    }

    @Override
    public boolean execute(String command) {
        return transaction(tx -> tx.execute(command));
    }

    @Override
    public void insert(Table table, Object data) {
        transactionV(tx -> tx.insert(table, data));
    }

    @Override
    public void insertMany(Table table, List<?> data) {
        transactionV(tx -> tx.insertMany(table, data));
    }

    @Override
    public List<Map<String, Object>> select(String query, Map<String, Object> params) {
        return transaction(tx -> tx.select(query, params));
    }

    @Override
    public List<Map<String, Object>> select(String query) {
        return transaction(tx -> tx.select(query));
    }

    @Override
    public Object selectSingle(String query) {
        return transaction(tx -> tx.selectSingle(query));
    }

    @Override
    public <T> List<T> select(Class<T> clazz, String query, Map<String, Object> params) {
        return transaction(tx -> tx.select(clazz, query, params));
    }

    @Override
    public <T> List<T> select(Class<T> clazz, Table table) {
        return transaction(tx -> tx.select(clazz, table));
    }

    @Override
    public <T> List<T> select(
        Class<T> clazz,
        Table table,
        String where,
        List<String> orderBy,
        Map<String, Object> params
    ) {
        return transaction(tx -> tx.select(clazz, table, where, orderBy, params));
    }

    @Override
    public <T> List<T> selectById(Class<T> clazz, Table table, Collection<Long> ids) {
        return transaction(tx -> tx.selectById(clazz, table, ids));
    }

    @Override
    public <T> T selectSingleById(Class<T> clazz, Table table, long id) {
        return transaction(tx -> tx.selectSingleById(clazz, table, id));
    }

    @Override
    public <T> void update(Table table, T data) {
        transactionV(tx -> tx.update(table, data));
    }

    @Override
    public <T> void updateMany(Table table, List<T> data) {
        transactionV(tx -> tx.updateMany(table, data));
    }

    @Override
    public void delete(Table table, Collection<Long> ids) {
        transactionV(tx -> tx.delete(table, ids));
    }

    @Override
    public void delete(Table table, long id) {
        transactionV(tx -> tx.delete(table, id));
    }

    private void enableForeignKeys(Transaction tx) {
        tx.execute("PRAGMA foreign_keys=1");
        if ((Integer) tx.selectSingle("PRAGMA foreign_keys") != 1) {
            throw new Exn("Cannot set foreign_keys=1.");
        }
    }

    private void commit(Transaction tx) {
        try {
            tx.execute("COMMIT");
        } catch (Exception ex) {
            log.warn(ex.getMessage(), ex);
        }
    }

    private void rollback(Transaction tx) {
        try {
            tx.execute("ROLLBACK");
        } catch (Exception ex) {
            log.warn(ex.getMessage(), ex);
        }
    }
}
