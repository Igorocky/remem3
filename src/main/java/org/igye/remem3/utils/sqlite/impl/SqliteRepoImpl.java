package org.igye.remem3.utils.sqlite.impl;

import lombok.SneakyThrows;
import org.apache.commons.dbcp2.BasicDataSource;
import org.igye.remem3.utils.RememExn;
import org.igye.remem3.utils.sqlite.SqliteRepo;
import org.igye.remem3.utils.sqlite.SqliteTransaction;

import java.sql.Connection;
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
            try {
                enableForeignKeys(tx);
                tx.execute("BEGIN DEFERRED TRANSACTION");
                return consumer.apply(tx);
            } catch (Throwable th) {
                tx.execute("ROLLBACK TRANSACTION");
                throw th;
            } finally {
                tx.execute("COMMIT TRANSACTION");
            }
        }
    }

    private void enableForeignKeys(SqliteTransaction tx) {
        tx.execute("PRAGMA foreign_keys = ON");
        if ((Integer) (tx.executeQuery("PRAGMA foreign_keys").getFirst().get("foreign_keys")) != 1) {
            throw new RememExn("Cannot set foreign_keys=1.");
        }
    }
}
