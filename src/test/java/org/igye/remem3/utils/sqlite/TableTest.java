package org.igye.remem3.utils.sqlite;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.igye.remem3.utils.sqlite.ColumnType.*;
import static org.igye.remem3.utils.sqlite.ForeignKeyAction.CASCADE;

class TableTest {
    @Test
    void testSchemaGeneration() {
        List<Table> tables = new ArrayList<>();

        tables.add(
            Table.builder()
                .name("CACHE")
                .columns(List.of(
                    Column.builder().name("key").type(TEXT).unique(true).build(),
                    Column.builder().name("value").type(TEXT).build()
                ))
                .build()
        );

        Table languageTable = Table.builder()
            .name("LANGUAGE")
            .columns(List.of(
                Column.builder().name("name").type(TEXT).unique(true).build()
            ))
            .build();
        tables.add(languageTable);

        Column parentIdCol = Column.builder().name("parent_id").type(INTEGER).notNull(false).build();
        Table folderTable = Table.builder()
            .name("FOLDER")
            .columns(List.of(
                parentIdCol,
                Column.builder().name("name").type(TEXT).build()
            ))
            .build();
        parentIdCol.setForeignKey(ForeignKey.builder().table(folderTable).build());
        tables.add(folderTable);

        Table cardTypeTable = Table.builder()
            .name("CARD_TYPE")
            .columns(List.of(
                Column.builder().name("code").type(TEXT).unique(true).build(),
                Column.builder().name("table_name").type(TEXT).unique(true).build()
            ))
            .build();
        tables.add(cardTypeTable);

        Table taskTypeTable = Table.builder()
            .name("TASK_TYPE")
            .columns(List.of(
                Column.builder().name("card_type_id").type(INTEGER)
                    .foreignKey(ForeignKey.builder().table(cardTypeTable).build())
                    .build(),
                Column.builder().name("code").type(TEXT).unique(true).build()
            ))
            .build();
        tables.add(taskTypeTable);

        Table cardTable = Table.builder()
            .name("CARD")
            .columns(List.of(
                Column.builder().name("folder_id").type(INTEGER)
                    .foreignKey(ForeignKey.builder().table(folderTable).build())
                    .build(),
                Column.builder().name("card_type_id").type(INTEGER)
                    .foreignKey(ForeignKey.builder().table(cardTypeTable).build())
                    .build(),
                Column.builder().name("crt_time").type(INTEGER).defaultValue("unixepoch()").build()
            ))
            .trackHistory(true)
            .build();
        tables.add(cardTable);

        tables.add(
            Table.builder()
                .name("TASK_HIST")
                .columns(List.of(
                    Column.builder().name("time").type(INTEGER).defaultValue("unixepoch()").build(),
                    Column.builder().name("card_id").type(INTEGER)
                        .foreignKey(ForeignKey.builder().table(cardTable).build())
                        .build(),
                    Column.builder().name("task_type_id").type(INTEGER)
                        .foreignKey(ForeignKey.builder().table(taskTypeTable).build())
                        .build(),
                    Column.builder().name("mark").type(REAL).check("0 <= ${thisColumn} and ${thisColumn} <= 1").build(),
                    Column.builder().name("note").type(TEXT).notNull(false).build()
                ))
                .build()
        );

        tables.add(
            Table.builder()
                .name("CARD_TRAN")
                .columns(List.of(
                    Column.builder().name("card_id").type(INTEGER)
                        .foreignKey(ForeignKey.builder().table(cardTable).onDelete(CASCADE).build())
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
                .build()
        );

        tables.add(
            Table.builder()
                .name("CARD_FILL")
                .columns(List.of(
                    Column.builder().name("card_id").type(INTEGER)
                        .foreignKey(ForeignKey.builder().table(cardTable).onDelete(CASCADE).build())
                        .build(),
                    Column.builder().name("lang_id").type(INTEGER)
                        .foreignKey(ForeignKey.builder().table(languageTable).build())
                        .build(),
                    Column.builder().name("descr").type(TEXT).build(),
                    Column.builder().name("text").type(TEXT).build(),
                    Column.builder().name("notes").type(TEXT).build()
                ))
                .trackHistory(true)
                .build()
        );

        for (Table table : tables) {
            for (String stmt : table.getSqlText()) {
                System.out.println("\n");
                System.out.println(stmt);
            }
            System.out.println("\n");
            System.out.println("---------------------------------------------------------------------");
        }
    }
}