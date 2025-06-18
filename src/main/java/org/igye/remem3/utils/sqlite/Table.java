package org.igye.remem3.utils.sqlite;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Table {
    private String name;
    @Builder.Default
    private String idColumnName = "id";
    private List<Column> columns;
    private boolean trackHistory;

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
            res.addAll(Table.builder().name(histTableName).idColumnName("_hist_id").columns(cols).build().getSqlText());
            String unprefixedColNames = getListOfPrefixedColNames("");
            String newColNames = getListOfPrefixedColNames("new.");
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
                    getListOfPrefixedColNames("old.")
                )
            );
        }
        return res;
    }

    private String getListOfPrefixedColNames(String prefix) {
        StringBuilder sb = new StringBuilder(prefix).append(idColumnName);
        for (Column col : columns) {
            sb.append(", ").append(prefix).append(col.getName());
        }
        return sb.toString();
    }
}
