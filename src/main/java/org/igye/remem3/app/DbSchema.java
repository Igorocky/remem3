package org.igye.remem3.app;

import org.igye.remem3.utils.sqlite.Table;
import org.igye.remem3.utils.sqlite.Transaction;

import java.util.List;

public interface DbSchema {
    int getVersion();

    void upgrade(Transaction tx);

    List<Table> getAllTables();

    Table getCacheTable();

    Table getLanguageTable();

    Table getFolderTable();

    Table getCardTypeTable();

    Table getTaskTypeTable();

    Table getCardTable();

    Table getCardTranTable();

    Table getCardFillTable();

    Table getTaskHistTable();
}
