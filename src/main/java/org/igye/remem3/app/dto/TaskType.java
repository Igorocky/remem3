package org.igye.remem3.app.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

public sealed interface TaskType permits TaskType.FillGaps {
    String getCode();

    @Getter
    @Builder
    @EqualsAndHashCode
    @ToString
    final class FillGaps implements TaskType {
        private String lang;

        @Override
        public String getCode() {
            return "FillGaps:" + lang;
        }
    }
}
