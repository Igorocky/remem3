package org.igye.remem3.app.controllers.cardimporter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;
import org.igye.remem3.app.components.DirSelectorCmp;
import org.igye.remem3.app.imprt.CardCollectionDto;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Builder
@With
@Getter
public class CardImporterState {
    private final DirSelectorCmp dir;
    @Builder.Default
    private final List<String> errors = List.of();
    @Builder.Default
    private final String jsonText = "";
    @Builder.Default
    private final Optional<CardCollectionDto> collection = Optional.empty();
    @Builder.Default
    private final String source = "";
    @Builder.Default
    private final String rootChapterName = "";
    @Builder.Default
    private final boolean skipRootDir = false;
    @Builder.Default
    private final Optional<ChapterView> chapterTree = Optional.empty();
    @Builder.Default
    private final Optional<ImportResult> importResult = Optional.empty();
}
