package org.igye.remem3.utils.sqlite;

import java.util.function.Consumer;
import java.util.function.Function;

public interface SqliteRepo {
    <T> T transaction(Function<SqliteTransaction, T> consumer);

    void transactionV(Consumer<SqliteTransaction> consumer);
}
