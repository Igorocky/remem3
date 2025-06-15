package org.igye.remem3.utils.sqlite;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ForeignKey {
    private Table table;
    @Builder.Default
    private ForeignKeyAction onUpdate = ForeignKeyAction.RESTRICT;
    @Builder.Default
    private ForeignKeyAction onDelete = ForeignKeyAction.RESTRICT;

}
