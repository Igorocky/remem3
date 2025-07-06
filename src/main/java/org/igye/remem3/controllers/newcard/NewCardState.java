package org.igye.remem3.controllers.newcard;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.controllers.components.DirSelectorCmp;

import java.util.List;

@Builder
@Getter
@With
@EqualsAndHashCode
@ToString
public class NewCardState {
    private List<String> errors;
    private Settings settings;
    private Cache cache;
    private DirSelectorCmp dirSelector;
    private CardDto cardParams;
}
