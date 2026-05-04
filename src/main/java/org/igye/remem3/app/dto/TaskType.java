package org.igye.remem3.app.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

public sealed interface TaskType permits TaskType.FillGaps, TaskType.Translate {
    String getCode();

    @EqualsAndHashCode
    @ToString
    final class FillGaps implements TaskType {
        @Getter
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

    @EqualsAndHashCode
    @ToString
    final class Translate implements TaskType {
        @Getter
        private final String langFrom;
        @Getter
        private final String langTo;
        private String code;

        public Translate(String langFrom, String langTo) {
            this.langFrom = langFrom;
            this.langTo = langTo;
        }

        @Override
        public String getCode() {
            if (code == null) {
                code = String.format("translate:%s->%s", langFrom, langTo);
            }
            return code;
        }
    }
}
