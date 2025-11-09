package org.igye.remem3.app.controllers.newcard;

import lombok.Builder;
import lombok.Getter;
import lombok.With;
import org.igye.remem3.app.dto.CardType;

import static org.igye.remem3.app.dto.CardType.FILL_GAPS;
import static org.igye.remem3.app.dto.CardType.TRANSLATE;

public sealed interface CardDto {
    CardType getType();

    @Builder
    @Getter
    @With
    final class FillGaps implements CardDto {
        private String lang;
        private String text;
        private String notes;

        @Override
        public CardType getType() {
            return FILL_GAPS;
        }
    }

    @Builder
    @Getter
    @With
    final class Translate implements CardDto {
        private String lang1;
        private String text1;
        private boolean exactMatch1;
        private String lang2;
        private String text2;
        private boolean exactMatch2;
        private String notes;

        @Override
        public CardType getType() {
            return TRANSLATE;
        }
    }
}