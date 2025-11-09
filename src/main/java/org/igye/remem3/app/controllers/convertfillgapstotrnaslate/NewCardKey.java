package org.igye.remem3.app.controllers.convertfillgapstotrnaslate;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@Builder
@Getter
public class NewCardKey {
    private String origFileUniqueName;
    private String gapAns;

    @Override
    public String toString() {
        return String.format("%s:%s", origFileUniqueName.trim(), gapAns.trim());
    }
}
