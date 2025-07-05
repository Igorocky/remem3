package org.igye.remem3.app.dto.fillgaps;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.HistRec;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Builder
@Getter
@ToString
@EqualsAndHashCode
@With
public class CardFillGaps implements Card {
    @Builder.Default
    private Optional<File> file = Optional.empty();
    @Builder.Default
    private Optional<Instant> createdAt = Optional.empty();
    @Builder.Default
    private String lang = "";
    @Builder.Default
    private List<TextPart> text = List.of();
    @Builder.Default
    private String notes = "";
    @Builder.Default
    private List<HistRec> history = List.of();
}
