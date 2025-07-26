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

    void copyFrom(Card other);

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
        public void copyFrom(Card other) {
            file = other.getFile();
            createdAt = other.getCreatedAt();
            history = new ArrayList<>(other.getHistory());
            taskTypes = null;
            tasks = null;
            childCopyFrom(other);
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
                tasks = getTaskTypes().stream()
                    .map(taskType -> new Task(this, taskType))
                    .toList();
            }
            return tasks;
        }

        abstract protected void childCopyFrom(Card other);

        abstract protected List<TaskType> makeTaskTypes();
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
        private String descr = "";
        @Getter
        @Builder.Default
        private List<TextPart> text = List.of();
        @Getter
        @Builder.Default
        private String notes = "";

        @Override
        protected void childCopyFrom(Card card) {
            FillGaps other = (FillGaps) card;
            lang = other.getLang();
            descr = other.getDescr();
            text = other.getText();
            notes = other.getNotes();
        }

        @Override
        protected List<TaskType> makeTaskTypes() {
            return List.of(new TaskType.FillGaps(lang));
        }
    }

    @SuperBuilder
    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    final class Translate extends BaseCard {
        @Getter
        @Builder.Default
        private String lang1 = "";
        @Getter
        @Builder.Default
        private String text1 = "";
        @Getter
        @Builder.Default
        private boolean exactMatch1 = true;
        @Getter
        @Builder.Default
        private String lang2 = "";
        @Getter
        @Builder.Default
        private String text2 = "";
        @Getter
        @Builder.Default
        private boolean exactMatch2 = true;
        @Getter
        @Builder.Default
        private String notes = "";

        @Override
        protected void childCopyFrom(Card card) {
            Translate other = (Translate) card;
            lang1 = other.getLang1();
            text1 = other.getText1();
            exactMatch1 = other.isExactMatch1();
            lang2 = other.getLang2();
            text2 = other.getText2();
            exactMatch2 = other.isExactMatch2();
            notes = other.getNotes();
        }

        @Override
        protected List<TaskType> makeTaskTypes() {
            return List.of(
                new TaskType.Translate(lang1, lang2),
                new TaskType.Translate(lang2, lang1)
            );
        }
    }

}
