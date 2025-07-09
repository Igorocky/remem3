package org.igye.remem3.app.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.igye.remem3.app.dto.fillgaps.TextPart;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public sealed interface Card {
    Optional<File> getFile();

    Optional<Instant> getCreatedAt();

    List<HistRec> getHistory();

    List<TaskType> getTaskTypes();

    List<Task> getTasks();

    void appendHistRec(HistRec histRec);

    @SuperBuilder
    @ToString(exclude = {"taskTypes", "tasks"})
    @EqualsAndHashCode(exclude = {"taskTypes", "tasks"})
    sealed abstract class BaseCard implements Card {
        @Getter
        @Builder.Default
        private Optional<File> file = Optional.empty();
        @Getter
        @Builder.Default
        private Optional<Instant> createdAt = Optional.empty();
        @Getter
        @Builder.Default
        private List<HistRec> history = List.of();
        private List<TaskType> taskTypes;
        private List<Task> tasks;

        @Override
        public void appendHistRec(HistRec histRec) {
            if (this.history instanceof ArrayList<HistRec>) {
                this.history.add(histRec);
            } else {
                this.history = new ArrayList<>(this.history);
                appendHistRec(histRec);
            }
        }

        @Override
        public List<TaskType> getTaskTypes() {
            if (taskTypes == null) {
                taskTypes = makeTaskTypes();
            }
            return taskTypes;
        }

        @Override
        public List<Task> getTasks() {
            if (tasks == null) {
                tasks = makeTasks();
            }
            return tasks;
        }

        abstract protected List<TaskType> makeTaskTypes();

        abstract protected List<Task> makeTasks();
    }

    @SuperBuilder
    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    final class FillGaps extends BaseCard {
        @Getter
        @Builder.Default
        private String lang = "";
        @Getter
        @Builder.Default
        private List<TextPart> text = List.of();
        @Getter
        @Builder.Default
        private String notes = "";

        @Override
        protected List<TaskType> makeTaskTypes() {
            return List.of(new TaskType.FillGaps(lang));
        }

        @Override
        protected List<Task> makeTasks() {
            return List.of(new Task.FillGaps(this));
        }
    }

}
