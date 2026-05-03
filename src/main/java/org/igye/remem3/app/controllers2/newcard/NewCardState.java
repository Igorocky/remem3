package org.igye.remem3.app.controllers2.newcard;

import lombok.Builder;
import lombok.Getter;
import lombok.With;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;

import java.util.List;

@Builder
@Getter
@With
public class NewCardState {
    private final List<String> errors;
    private final DirSelectorCmp dir;
    private final CardDto card;
}
