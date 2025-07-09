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

    List<Task> getTasks();

    @Builder
    @ToString
    @EqualsAndHashCode
    @With
    final class FillGaps implements Card {
        @Getter
        @Builder.Default
        private Optional<File> file = Optional.empty();
        @Getter
        @Builder.Default
        private Optional<Instant> createdAt = Optional.empty();
        @Getter
        @Builder.Default
        private String lang = "";
        @Getter
        @Builder.Default
        private List<TextPart> text = List.of();
        @Getter
        @Builder.Default
        private String notes = "";
        @Getter
        @Builder.Default
        private List<HistRec> history = List.of();
        private List<TaskType> taskTypes;
        private List<Task> tasks;

        @Override
        public List<TaskType> getTaskTypes() {
            if (taskTypes == null) {
                taskTypes = List.of(new TaskType.FillGaps(lang));
            }
            return taskTypes;
        }

        @Override
        public List<Task> getTasks() {
            if (tasks == null) {
                tasks = List.of(new Task.FillGaps(this));
            }
            return tasks;
        }
    }

}
