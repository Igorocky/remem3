package org.igye.remem3.app.dto.fillgaps;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

public sealed interface TextPart permits TextPart.Text, TextPart.Gap {
    @Builder
    @Getter
    @EqualsAndHashCode
    @ToString
    final class Text implements TextPart {
        private String text;
    }

    @Builder
    @Getter
    @EqualsAndHashCode
    @ToString
    final class Gap implements TextPart {
        private String answer;
        private String hint;
        private String notes;
    }
}
