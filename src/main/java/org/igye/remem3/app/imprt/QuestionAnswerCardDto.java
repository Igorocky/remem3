package org.igye.remem3.app.imprt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Builder
@Getter
@Jacksonized
public class QuestionAnswerCardDto implements CardDto {
    private final CardType type = CardType.QUESTION_ANSWER;
    private final String question;
    private final String answer;
}
