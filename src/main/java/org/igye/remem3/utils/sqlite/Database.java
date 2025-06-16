package org.igye.remem3.utils.sqlite;

import java.util.function.Consumer;
import java.util.function.Function;

public interface Database {
    <T> T transaction(Function<Transaction, T> consumer);

    void transactionV(Consumer<Transaction> consumer);
}
