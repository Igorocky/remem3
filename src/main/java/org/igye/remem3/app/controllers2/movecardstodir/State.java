package org.igye.remem3.app.controllers2.movecardstodir;

import lombok.Builder;
import lombok.Getter;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;
import org.igye.remem3.app.controllers.movecardstodir.Bundle;
import org.igye.remem3.app.dto.CardType;
import org.igye.remem3.app.dto.RepeatStrategyType;

import java.util.List;
import java.util.Set;

@Builder
@Getter
public class State {
    private List<String> errors;
    private DirSelectorCmp dirMoveFrom;
    private CardType cardType;
    private String lang;
    private RepeatStrategyType repeatStrategyType;
    private DirSelectorCmp dirMoveTo;
    private List<Bundle> sortedBundlesToList;
    private Set<String> selectedBundleIds;
}
