package org.igye.remem3.app.dto;

import lombok.EqualsAndHashCode;
import lombok.ToString;

public sealed interface TaskType permits TaskType.FillGaps {
    String getCode();

    @EqualsAndHashCode
    @ToString
    final class FillGaps implements TaskType {
        private final String lang;
        private String code;

        public FillGaps(String lang) {
            this.lang = lang;
        }

        @Override
        public String getCode() {
            if (code == null) {
                code = "FillGaps:" + lang;
            }
            return code;
        }
    }
}
