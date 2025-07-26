package org.igye.remem3.datamigration;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Database {
    <T> T transaction(Function<Transaction, T> consumer);

    void transactionV(Consumer<Transaction> consumer);

    boolean execute(String command);

    List<Map<String, Object>> select(String query, Map<String, Object> params);

    List<Map<String, Object>> select(String query);

    Object selectSingle(String query);
}
