package org.igye.remem3.app.db.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CardEnt {
    public Long id;
    public Long folderId;
    public Long cardTypeId;
    public Long crtTime;
}
