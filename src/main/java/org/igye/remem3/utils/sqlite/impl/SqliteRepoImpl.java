package org.igye.remem3.utils.sqlite.impl;

import lombok.SneakyThrows;
import org.apache.commons.dbcp2.BasicDataSource;
import org.igye.remem3.utils.RememExn;
import org.igye.remem3.utils.sqlite.SqliteRepo;
import org.igye.remem3.utils.sqlite.SqliteTransaction;

import java.sql.Connection;
import java.util.function.Consumer;
import java.util.function.Function;

public class SqliteRepoImpl implements SqliteRepo {
    private final BasicDataSource dataSource;

    public SqliteRepoImpl(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @SneakyThrows
    @Override
    public <T> T transaction(Function<SqliteTransaction, T> consumer) {
        try (Connection connection = dataSource.getConnection()) {
            SqliteTransaction tx = new SqliteTransactionImpl(connection);
            boolean commit = false;
            try {
                enableForeignKeys(tx);
                tx.execute("BEGIN DEFERRED TRANSACTION");
                T result = consumer.apply(tx);
                commit = true;
                return result;
            } catch (Throwable th) {
                tx.execute("ROLLBACK TRANSACTION");
                throw th;
            } finally {
                if (commit) {
                    tx.execute("COMMIT TRANSACTION");
                }
            }
        }
    }

    @Override
    public void transactionV(Consumer<SqliteTransaction> consumer) {
        transaction(tx -> {
            consumer.accept(tx);
            return null;
        });
    }

    private void enableForeignKeys(SqliteTransaction tx) {
        tx.execute("PRAGMA foreign_keys=1");
        if ((Integer) tx.selectSingle("PRAGMA foreign_keys") != 1) {
            throw new RememExn("Cannot set foreign_keys=1.");
        }
    }
}
