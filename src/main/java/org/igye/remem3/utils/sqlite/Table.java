package org.igye.remem3.utils.sqlite;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Builder
@Getter
public class Table {
    private String name;
    @Builder.Default
    private String idColumnName = "id";
    private List<Column> columns;
    private boolean trackHistory;
    private Table histTable;

    @Getter(AccessLevel.NONE)
    @Builder.Default
    private Map<String, String> prefixedColumns = new HashMap<>();

    public List<String> getSqlText() {
        StringBuilder sb = new StringBuilder("create table ").append(name).append(" (\n");
        sb.append("    ").append(idColumnName).append(" INTEGER primary key");
        for (String colDef : columns.stream().map(Column::getSqlText).toList()) {
            sb.append(",\n    ").append(colDef);
        }
        sb.append("\n) strict");
        ArrayList<String> res = new ArrayList<>();
        res.add(sb.toString());
        if (trackHistory) {
            ArrayList<Column> cols = new ArrayList<>();
            cols.add(Column.builder().name("_time").type(ColumnType.INTEGER).defaultValue("unixepoch()").build());
            cols.add(Column.builder().name("_act").type(ColumnType.INTEGER).check("${thisColumn} in (0,1,2)").build());
            cols.add(
                Column.builder()
                    .name(this.idColumnName)
                    .type(ColumnType.INTEGER)
                    .foreignKey(
                        ForeignKey.builder()
                            .table(this)
                            .onUpdate(ForeignKeyAction.RESTRICT)
                            .onDelete(ForeignKeyAction.NO_ACTION)
                            .build()
                    )
                    .build()
            );
            for (Column col : columns) {
                cols.add(Column.builder().name(col.getName()).type(col.getType()).notNull(false).build());
            }
            String histTableName = "_hist_" + name;
            this.histTable = Table.builder().name(histTableName).idColumnName("_hist_id").columns(cols).build();
            res.addAll(histTable.getSqlText());
            String unprefixedColNames = getPrefixedColumns("");
            String newColNames = getPrefixedColumns("new.");
            res.add(
                String.format(
                    """
                        create trigger %s after insert on %s FOR EACH ROW BEGIN
                            insert into %s (_act, %s) values (0, %s);
                        end""",
                    "_trg_ins_" + name,
                    name,
                    histTableName,
                    unprefixedColNames,
                    newColNames
                )
            );
            res.add(
                String.format(
                    """
                        create trigger %s after update on %s FOR EACH ROW BEGIN
                            insert into %s (_act, %s) values (1, %s);
                        end""",
                    "_trg_upd_" + name,
                    name,
                    histTableName,
                    unprefixedColNames,
                    newColNames
                )
            );
            res.add(
                String.format(
                    """
                        create trigger %s after delete on %s FOR EACH ROW BEGIN
                            insert into %s (_act, %s) values (2, %s);
                        end""",
                    "_trg_del_" + name,
                    name,
                    histTableName,
                    unprefixedColNames,
                    getPrefixedColumns("old.")
                )
            );
        }
        return res;
    }

    public String getPrefixedColumns(String prefix) {
        String res = prefixedColumns.get(prefix);
        if (res != null) {
            return res;
        }
        StringBuilder sb = new StringBuilder(prefix).append(idColumnName);
        for (Column col : columns) {
            sb.append(", ").append(prefix).append(col.getName());
        }
        res = sb.toString();
        prefixedColumns.put(prefix, res);
        return res;
    }
}
