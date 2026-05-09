package org.igye.remem3.app.controllers2.validatecards;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;
import org.igye.remem3.app.dto.Card;

import java.util.List;

@AllArgsConstructor
@Builder
@With
@Getter
public class ValidateCardsState {
    @Builder.Default
    private final List<String> errors = List.of();
    private final DirSelectorCmp dir;
    @Builder.Default
    private final List<Pair<Card, List<String>>> cardsAndErrors = List.of();

}
