package org.igye.remem3.app.controllers.cardexplorer;

import lombok.Builder;
import lombok.Getter;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;
import org.igye.remem3.app.dto.Card;

import java.util.List;

@Builder
@Getter
public class CardExplorerState {
    private final DirSelectorCmp dir;
    private List<Card> cards;
}
