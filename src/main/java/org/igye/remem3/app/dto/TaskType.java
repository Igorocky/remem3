package org.igye.remem3.app.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

public sealed interface TaskType {
    String getName();

    @Getter
    @Builder
    @EqualsAndHashCode
    @ToString
    final class FillGaps implements TaskType {
        public static final FillGaps FILL_GAPS = new FillGaps();

        @Override
        public String getName() {
            return "FillGaps";
        }
    }
}
