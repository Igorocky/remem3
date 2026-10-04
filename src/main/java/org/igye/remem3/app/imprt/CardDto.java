package org.igye.remem3.app.imprt;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = QuestionAnswerCardDto.class, name = "QUESTION_ANSWER"),
    @JsonSubTypes.Type(value = TranslationCardDto.class, name = "TRANSLATE")
})
public interface CardDto {
    CardType getType();
}
