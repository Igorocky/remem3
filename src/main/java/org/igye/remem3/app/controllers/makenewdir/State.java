package org.igye.remem3.app.controllers.makenewdir;

import lombok.Builder;
import lombok.Getter;
import lombok.With;
import org.igye.remem3.app.components.DirSelectorCmp;

import java.util.List;
import java.util.function.Supplier;

@Builder
@Getter
public class State {
    private Supplier<Object> onComplete;
    private Supplier<Object> onCancel;

    @Builder.Default
    @With
    private List<String> errors = List.of();
    @With
    private DirSelectorCmp parentDir;
    @With
    private String newDirName;
}
