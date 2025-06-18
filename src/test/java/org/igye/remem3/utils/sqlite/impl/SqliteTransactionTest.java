package org.igye.remem3.utils.sqlite.impl;

import org.igye.remem3.app.dbentity.LangEnt;
import org.igye.remem3.app.impl.DbSchemaImpl;
import org.igye.remem3.utils.sqlite.Database;
import org.junit.jupiter.api.Test;

import java.util.List;

class SqliteTransactionTest {
    @Test
    void insert() {
        //given
        Database db = SqliteDatabase.getInMemoryDb();
        DbSchemaImpl sc = new DbSchemaImpl();
        db.transactionV(sc::upgrade);

        db.transactionV(tx -> {
            List<Object> langs = List.of(
                LangEnt.builder().name("L1").build(),
                LangEnt.builder().name("L2").build(),
                LangEnt.builder().name("L3").build()
            );
            tx.insert(sc.getLanguageTable(), langs);
        });
    }
}