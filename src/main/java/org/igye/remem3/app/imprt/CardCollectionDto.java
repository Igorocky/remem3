package org.igye.remem3.app.imprt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class CardCollectionDto {
    private final String generatedBy;
    private final ChapterDto cards;
}
