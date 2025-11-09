package org.igye.remem3.app.controllers.validatecards;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.dto.Card;

import java.util.List;

@Builder
@Getter
@With
@EqualsAndHashCode
@ToString
public class ValidateCardsState {
    private List<String> errors;
    private String dir;
    private List<Pair<Card, List<String>>> cardsAndErrors;
}
