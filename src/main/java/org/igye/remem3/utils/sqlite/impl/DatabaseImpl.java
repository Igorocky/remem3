package org.igye.remem3.utils.sqlite.impl;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp2.BasicDataSource;
import org.igye.remem3.utils.RememExn;
import org.igye.remem3.utils.sqlite.ColumnToFieldMapping;
import org.igye.remem3.utils.sqlite.Database;
import org.igye.remem3.utils.sqlite.Transaction;

import java.sql.Connection;
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

    private void enableForeignKeys(Transaction tx) {
        tx.execute("PRAGMA foreign_keys=1");
        if ((Integer) tx.selectSingle("PRAGMA foreign_keys") != 1) {
            throw new RememExn("Cannot set foreign_keys=1.");
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
