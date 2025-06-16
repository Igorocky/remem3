package org.igye.remem3.utils.sqlite;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.igye.remem3.utils.RememExn;

@Builder
@Getter
public class Column {
    private String name;
    private ColumnType type;
    @Builder.Default
    private boolean notNull = true;
    private boolean unique;
    @Setter
    private ForeignKey foreignKey;
    private String defaultValue;
    private String check;

    public String getSqlText() {
        if (StringUtils.isBlank(name)) {
            throw new RememExn("Column name must not be blank.");
        }
        if (type == null) {
            throw new RememExn("Column type must not be null.");
        }
        StringBuilder sb = new StringBuilder(name).append(" ").append(type);
        if (notNull) {
            sb.append(" not null");
        }
        if (unique) {
            sb.append(" unique");
        }
        if (foreignKey != null) {
            if (foreignKey.getTable() == null) {
                throw new RememExn("Foreign key table must not be null.");
            }
            if (StringUtils.isBlank(foreignKey.getTable().getName())) {
                throw new RememExn("Foreign key table name must not be blank.");
            }
            if (foreignKey.getOnUpdate() == null) {
                throw new RememExn("Foreign key 'on update' must not be null.");
            }
            if (foreignKey.getOnDelete() == null) {
                throw new RememExn("Foreign key 'on delete' must not be null.");
            }
            sb.append(" references ").append(foreignKey.getTable().getName())
                .append(" on update ").append(foreignKey.getOnUpdate().getName())
                .append(" on delete ").append(foreignKey.getOnDelete().getName());
        }
        if (StringUtils.isNotBlank(defaultValue)) {
            sb.append(" default (").append(defaultValue).append(")");
        }
        if (StringUtils.isNotBlank(check)) {
            sb.append(" check (").append(check.replace("${thisColumn}", name)).append(")");
        }
        return sb.toString();
    }
}
