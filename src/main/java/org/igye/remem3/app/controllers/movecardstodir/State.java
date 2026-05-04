package org.igye.remem3.app.controllers.movecardstodir;

import lombok.Builder;
import lombok.Getter;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;
import org.igye.remem3.app.dto.CardType;
import org.igye.remem3.app.dto.RepeatStrategyType;
import org.igye.remem3.web.RequestParams;

import java.util.List;
import java.util.Set;

@Builder
@Getter
public class State {
    private RequestParams params;
    private List<String> errors;
    private DirSelectorCmp dirMoveFrom;
    private CardType cardType;
    private String lang;
    private RepeatStrategyType repeatStrategyType;
    private DirSelectorCmp dirMoveTo;
    private List<Bundle> sortedBundlesToList;
    private Set<String> selectedBundleIds;
}
