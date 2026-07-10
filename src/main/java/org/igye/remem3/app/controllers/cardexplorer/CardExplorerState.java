package org.igye.remem3.app.controllers.cardexplorer;

import lombok.Builder;
import lombok.Getter;
import lombok.With;
import org.igye.remem3.app.components.DirSelectorCmp;
import org.igye.remem3.app.dto.Card;

import java.util.List;
import java.util.Optional;

@Builder
@Getter
public class CardExplorerState {
    private final DirSelectorCmp dir;
    private final boolean sortAsc;
    private final boolean recursive;
    private final String filter;
    private List<Card> cards;
    @With
    @Builder.Default
    private Optional<String> scrollToId = Optional.empty();
}
