package org.igye.remem3.app.imprt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class QuestionAnswerCardDto implements CardDto {
    private final CardType type = CardType.QUESTION_ANSWER;
    private final String question;
    private final String answer;
}
