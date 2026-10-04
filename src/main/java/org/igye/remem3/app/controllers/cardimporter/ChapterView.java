package org.igye.remem3.app.controllers.cardimporter;

import lombok.Builder;
import lombok.Getter;
import org.igye.remem3.app.imprt.CardType;

import java.util.List;
import java.util.Map;

@Builder
@Getter
public class ChapterView {
    private final String chapterName;
    private final String dirName;
    private final String dirError;
    private final boolean dirSkipped;
    private final boolean dirExists;
    private final Map<CardType, Long> cardCounts;
    private final List<ChapterView> chapters;

    public long getTotalNumOfCards() {
        return cardCounts.values().stream().mapToLong(Long::longValue).sum()
            + chapters.stream().mapToLong(ChapterView::getTotalNumOfCards).sum();
    }
}
