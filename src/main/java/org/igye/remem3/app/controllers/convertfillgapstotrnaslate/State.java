package org.igye.remem3.app.controllers.convertfillgapstotrnaslate;

import lombok.Builder;
import lombok.Getter;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;

import java.util.List;

@Builder
@Getter
public class State {
    private List<String> errors;
    private DirSelectorCmp dirSelector;
    private String gapSecondLang;
}
