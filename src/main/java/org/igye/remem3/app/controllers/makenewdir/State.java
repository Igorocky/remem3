package org.igye.remem3.app.controllers.makenewdir;

import lombok.Builder;
import lombok.Getter;
import lombok.With;
import org.igye.remem3.app.components.DirSelectorCmp;

import java.io.File;
import java.util.List;
import java.util.function.Function;

@Builder
@Getter
public class State {
    private Function<File, Object> onComplete;
    private Object onCancel;

    @Builder.Default
    @With
    private List<String> errors = List.of();
    @With
    private DirSelectorCmp parentDir;
    @Builder.Default
    @With
    private String newDirName = "";
}
