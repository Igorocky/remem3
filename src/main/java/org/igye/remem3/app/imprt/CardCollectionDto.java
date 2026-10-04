package org.igye.remem3.app.imprt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Builder
@Getter
@Jacksonized
public class CardCollectionDto {
    private final String generatedBy;
    private final ChapterDto cards;
}
