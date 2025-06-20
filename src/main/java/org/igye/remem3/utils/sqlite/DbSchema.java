package org.igye.remem3.utils.sqlite;

public interface DbSchema {
    void upgrade(Transaction tx);
}
