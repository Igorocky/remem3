package org.igye.remem3.app.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import org.igye.remem3.app.dto.fillgaps.TextPart;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public sealed interface Card {
    Optional<File> getFile();

    Optional<Instant> getCreatedAt();

    List<HistRec> getHistory();

    List<TaskType> getTaskTypes();

    @Builder
    @Getter
    @ToString
    @EqualsAndHashCode
    @With
    final class FillGaps implements Card {
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

        @Override
        public List<TaskType> getTaskTypes() {
            return List.of(new TaskType.FillGaps(lang));
        }
    }

}
