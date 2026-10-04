package org.igye.remem3.app.imprt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@AllArgsConstructor
@Builder
@Getter
@Jacksonized
public class ChapterDto {
    private final String chapterName;
    private final List<ChapterDto> chapters;
    private final List<CardDto> cards;
}
