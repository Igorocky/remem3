package org.igye.remem3.app.dto.fillgaps;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class Text implements TextPart {
    private String text;
}
