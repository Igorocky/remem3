package org.igye.remem3.datamigration;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp2.BasicDataSource;
import org.igye.remem3.utils.Exn;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class DatabaseImpl implements Database {
    private final BasicDataSource dataSource;

    public static DatabaseImpl getInMemoryDb() {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("org.sqlite.JDBC");
        ds.setUrl("jdbc:sqlite::memory:");
        return new DatabaseImpl(ds);
    }

    public DatabaseImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;

        transactionV(tx -> {
            List<Map<String, Object>> violations = tx.select("PRAGMA foreign_key_check");
            if (!violations.isEmpty()) {
                throw new Exn("There are foreign key violations in the database.");
            }
        });
    }

    @SneakyThrows
    @Override
    public <T> T transaction(Function<Transaction, T> consumer) {
        try (Connection connection = dataSource.getConnection()) {
            Transaction tx = new TransactionImpl(connection);
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
