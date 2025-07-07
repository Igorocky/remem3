package org.igye.remem3.controllers.exercise;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.dto.TaskType;
import org.igye.remem3.app.impl.CardsImpl;
import org.igye.remem3.controllers.components.DirSelectorCmp;

import java.util.List;

@Builder
@Getter
@With
@EqualsAndHashCode
@ToString
public class ExerciseState {
    private List<String> errors;
    private CardsImpl cards;
    private DirSelectorCmp dirSelector;
    private List<Pair<TaskType, Boolean>> taskTypes;
}
