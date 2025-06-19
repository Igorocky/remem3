package org.igye.remem3.app.db.impl;

import lombok.Getter;
import org.igye.remem3.app.db.DbSchema;
import org.igye.remem3.utils.sqlite.Column;
import org.igye.remem3.utils.sqlite.ForeignKey;
import org.igye.remem3.utils.sqlite.Table;
import org.igye.remem3.utils.sqlite.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.igye.remem3.utils.sqlite.ColumnType.INTEGER;
import static org.igye.remem3.utils.sqlite.ColumnType.REAL;
import static org.igye.remem3.utils.sqlite.ColumnType.TEXT;
import static org.igye.remem3.utils.sqlite.ForeignKeyAction.CASCADE;

public class DbSchemaImpl implements DbSchema {
    public static final String LANG_NAME = "name";
    private static final String USER_VERSION = "user_version";
    @Getter
    private final Table cacheTable;
    @Getter
    private final Table languageTable;
    @Getter
    private final Table folderTable;
    @Getter
    private final Table cardTypeTable;
    @Getter
    private final Table taskTypeTable;
    @Getter
    private final Table cardTable;
    @Getter
    private final Table cardTranTable;
    @Getter
    private final Table cardFillTable;
    @Getter
    private final Table taskHistTable;
    @Getter
    private final List<Table> allTables;

    public DbSchemaImpl() {
        List<Table> allTables = new ArrayList<>();
        cacheTable = Table.builder()
            .name("CACHE")
            .columns(List.of(
                Column.builder().name("key").type(TEXT).unique(true).build(),
                Column.builder().name("value").type(TEXT).build()
            ))
            .build();
        allTables.add(cacheTable);

        languageTable = Table.builder()
            .name("LANGUAGE")
            .columns(List.of(
                Column.builder().name(LANG_NAME).type(TEXT).unique(true).build()
            ))
            .build();
        allTables.add(languageTable);

        Column parentIdCol = Column.builder().name("parent_id").type(INTEGER).notNull(false).build();
        folderTable = Table.builder()
            .name("FOLDER")
            .columns(List.of(
                parentIdCol,
                Column.builder().name("name").type(TEXT).build()
            ))
            .build();
        parentIdCol.setForeignKey(ForeignKey.builder().table(folderTable).build());
        allTables.add(folderTable);

        cardTypeTable = Table.builder()
            .name("CARD_TYPE")
            .columns(List.of(
                Column.builder().name("code").type(TEXT).unique(true).build(),
                Column.builder().name("table_name").type(TEXT).unique(true).build()
            ))
            .build();
        allTables.add(cardTypeTable);

        taskTypeTable = Table.builder()
            .name("TASK_TYPE")
            .columns(List.of(
                Column.builder().name("card_type_id").type(INTEGER)
                    .foreignKey(ForeignKey.builder().table(cardTypeTable).build())
                    .build(),
                Column.builder().name("code").type(TEXT).unique(true).build()
            ))
            .build();
        allTables.add(taskTypeTable);

        cardTable = Table.builder()
            .name("CARD")
            .columns(List.of(
                Column.builder().name("folder_id").type(INTEGER)
                    .foreignKey(ForeignKey.builder().table(folderTable).build())
                    .build(),
                Column.builder().name("card_type_id").type(INTEGER)
                    .foreignKey(ForeignKey.builder().table(cardTypeTable).build())
                    .build(),
                Column.builder().name("crt_time").type(INTEGER).defaultValue("unixepoch('subsec') * 1000").build()
            ))
            .trackHistory(true)
            .build();
        allTables.add(cardTable);

        taskHistTable = Table.builder()
            .name("TASK_HIST")
            .columns(List.of(
                Column.builder().name("time").type(INTEGER).defaultValue("unixepoch('subsec') * 1000").build(),
                Column.builder().name("card_id").type(INTEGER)
                    .foreignKey(ForeignKey.builder().table(cardTable).build())
                    .build(),
                Column.builder().name("task_type_id").type(INTEGER)
                    .foreignKey(ForeignKey.builder().table(taskTypeTable).build())
                    .build(),
                Column.builder().name("mark").type(REAL).check("0 <= ${thisColumn} and ${thisColumn} <= 1").build(),
                Column.builder().name("note").type(TEXT).notNull(false).build()
            ))
            .build();
        allTables.add(taskHistTable);

        cardTranTable = Table.builder()
            .name("CARD_TRAN")
            .columns(List.of(
                Column.builder().name("card_id").type(INTEGER)
                    .foreignKey(ForeignKey.builder().table(cardTable).onDelete(CASCADE).build())
                    .unique(true)
                    .build(),
                Column.builder().name("lang1_id").type(INTEGER)
                    .foreignKey(ForeignKey.builder().table(languageTable).build())
                    .build(),
                Column.builder().name("read_only1").type(INTEGER).check("${thisColumn} in (0,1)").build(),
                Column.builder().name("text1").type(TEXT).build(),
                Column.builder().name("lang2_id").type(INTEGER)
                    .foreignKey(ForeignKey.builder().table(languageTable).build())
                    .build(),
                Column.builder().name("read_only2").type(INTEGER).check("${thisColumn} in (0,1)").build(),
                Column.builder().name("text2").type(TEXT).build(),
                Column.builder().name("notes").type(TEXT).build()
            ))
            .trackHistory(true)
            .build();
        allTables.add(cardTranTable);

        cardFillTable = Table.builder()
            .name("CARD_FILL")
            .columns(List.of(
                Column.builder().name("card_id").type(INTEGER)
                    .foreignKey(ForeignKey.builder().table(cardTable).onDelete(CASCADE).build())
                    .unique(true)
                    .build(),
                Column.builder().name("lang_id").type(INTEGER)
                    .foreignKey(ForeignKey.builder().table(languageTable).build())
                    .build(),
                Column.builder().name("descr").type(TEXT).build(),
                Column.builder().name("text").type(TEXT).build(),
                Column.builder().name("notes").type(TEXT).build()
            ))
            .trackHistory(true)
            .build();
        allTables.add(cardFillTable);
        this.allTables = Collections.unmodifiableList(allTables);
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public void upgrade(Transaction tx) {
        int actualSchemaVersion = getActualSchemaVersion(tx);
        if (actualSchemaVersion == getVersion()) {
            return;
        }
        if (actualSchemaVersion == 0) {
            allTables.stream()
                .flatMap(table -> table.getSqlText().stream())
                .forEach(tx::execute);
            setSchemaVersion(tx, getVersion());
        }
    }

    private int getActualSchemaVersion(Transaction tx) {
        return (int) tx.selectSingle(String.format("PRAGMA %s", USER_VERSION));
    }

    private void setSchemaVersion(Transaction tx, int version) {
        tx.execute(String.format("PRAGMA %s=%s", USER_VERSION, version));
    }
}
