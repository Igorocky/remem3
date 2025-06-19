package org.igye.remem3.app.db.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CardTypeEnt {
    public Long id;
    public String code;
    public String tableName;
}
