package org.igye.remem3.app.controllers.exercise;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import org.igye.remem3.app.dto.Card;

import java.util.List;

@AllArgsConstructor
@Getter
@With
public class ExerciseCardEdit {
    private final Card card;
    //the content of the card file at the moment the editing started
    private final String originalText;
    private final String text;
    private final List<String> errors;
}
