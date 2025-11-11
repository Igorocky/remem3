package org.igye.remem3.app.controllers.convertfillgapstotrnaslate;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.web.RequestParams;

import java.util.List;
import java.util.Set;

@Builder
@Getter
public class State {
    private RequestParams params;
    private List<String> errors;
    private DirSelectorCmp dirSelector;
    private String gapSecondLang;
    private List<Pair<NewCardKey, Card.Translate>> newTranslateCards;
    private Set<NewCardKey> selectedCardKeys;
}
