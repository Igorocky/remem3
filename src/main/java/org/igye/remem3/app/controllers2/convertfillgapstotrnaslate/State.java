package org.igye.remem3.app.controllers2.convertfillgapstotrnaslate;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;
import org.igye.remem3.app.controllers.convertfillgapstotrnaslate.NewCardKey;
import org.igye.remem3.app.dto.Card;

import java.util.List;
import java.util.Set;

@Builder
@Getter
public class State {
    private List<String> errors;
    private DirSelectorCmp dir;
    private String gapSecondLang;
    private List<Pair<NewCardKey, Card.Translate>> newTranslateCards;
    private Set<NewCardKey> selectedCardKeys;
}
