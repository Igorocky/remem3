package org.igye.remem3.app.dto;

import lombok.EqualsAndHashCode;
import lombok.ToString;

public sealed interface TaskType {
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
                code = "fill_gaps:" + lang;
            }
            return code;
        }
    }
}
