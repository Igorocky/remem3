package org.igye.remem3.app.controllers.newcard;

import lombok.Builder;
import lombok.Getter;
import lombok.With;
import org.igye.remem3.app.dto.CardType;

import java.util.HashMap;
import java.util.Map;

import static org.igye.remem3.app.dto.CardType.FILL_GAPS;
import static org.igye.remem3.app.dto.CardType.TRANSLATE;

public sealed interface CardDto permits CardDto.FillGaps, CardDto.Translate {
    CardType getType();

    Map<String, String> getAttrs();

    @Builder
    @Getter
    @With
    final class FillGaps implements CardDto {
        private final String lang;
        private final String text;
        private final String notes;
        @Builder.Default
        private Map<String, String> attrs = new HashMap<>();

        @Override
        public CardType getType() {
            return FILL_GAPS;
        }
    }

    @Builder
    @Getter
    @With
    final class Translate implements CardDto {
        private final String lang1;
        private final String text1;
        private final boolean exactMatch1;
        private final String example1;
        private final String lang2;
        private final String text2;
        private final boolean exactMatch2;
        private final String example2;
        private final String notes;
        @Builder.Default
        private Map<String, String> attrs = new HashMap<>();

        @Override
        public CardType getType() {
            return TRANSLATE;
        }
    }
}