package org.igye.remem3.app.controllers.cardexplorer;

import lombok.Builder;
import lombok.Getter;
import org.igye.remem3.app.components.DirSelectorCmp;
import org.igye.remem3.app.dto.Card;

import java.util.List;

@Builder
@Getter
public class CardExplorerState {
    private final DirSelectorCmp dir;
    private final boolean sortAsc;
    private final boolean recursive;
    private final String filter;
    private List<Card> cards;
}
