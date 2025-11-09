package org.igye.remem3.app.controllers.convertfillgapstotrnaslate;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Builder
@Getter
public class NewCardKey {
    private String origFileUniqueName;
    private String gapAns;
}
