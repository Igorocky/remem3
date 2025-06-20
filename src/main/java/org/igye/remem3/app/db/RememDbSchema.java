package org.igye.remem3.app.db;

import org.igye.remem3.utils.sqlite.DbSchema;
import org.igye.remem3.utils.sqlite.Table;

public interface RememDbSchema extends DbSchema {

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
