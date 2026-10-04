package org.igye.remem3.app.imprt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Builder
@Getter
@Jacksonized
public class TranslationCardDto implements CardDto {
    private final CardType type = CardType.TRANSLATE;
    private final String fromLanguage;
    private final String textToTranslate;
    private final String toLanguage;
    private final String translatedText;
}
