package org.igye.remem3.app.controllers.convertfillgapstotrnaslate;

import lombok.Builder;
import lombok.Getter;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;

@Builder
@Getter
public class State {
    private DirSelectorCmp dirSelector;
}
