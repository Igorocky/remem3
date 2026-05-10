package org.igye.remem3.app.controllers2.exercise;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.controllers2.beans.dto.ExerciseDef;
import org.igye.remem3.app.state.StateRenderer;
import org.igye.remem3.app.taskstate.TaskState;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlText;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

@RequiredArgsConstructor
public class ExerciseRenderer extends HtmlBuilder implements StateRenderer<ExerciseState> {
    public static final String PAR_SELECTED_EXERCISE = "Exercise_PAR_SELECTED_EXERCISE";
    public static final String PAR_SELECTED_DIR = "Exercise_PAR_SELECTED_DIR";
    public static final String PAR_SELECTED_TASK_FILTER = "Exercise_PAR_SELECTED_TASK_FILTER";
    public static final String PAR_SELECTED_STRATEGY = "Exercise_PAR_SELECTED_STRATEGY";
    public static final String PAR_SHOW_EXERCISE_PARAMS = "Exercise_PAR_SHOW_EXERCISE_PARAMS";
    public static final String PAR_SHOW_DAILY_UNIQUE_COUNT = "Exercise_PAR_SHOW_DAILY_UNIQUE_COUNT";
    public static final String ACT_START_EXERCISE = "Exercise_ACT_START_EXERCISE";
    public static final String ACT_CANCEL_EXERCISE = "Exercise_ACT_CANCEL_EXERCISE";
    public static final String ACT_REFRESH_EXERCISE = "Exercise_ACT_REFRESH_EXERCISE";
    public static final String ACT_TOGGLE_SHOW_EXERCISE_PARAMS = "Exercise_ACT_TOGGLE_SHOW_EXERCISE_PARAMS";
    public static final String ACT_TOGGLE_SHOW_LESS_MORE_EXERCISE_PARAMS = "Exercise_ACT_TOGGLE_SHOW_LESS_MORE_EXERCISE_PARAMS";
    public static final String ACT_TOGGLE_SHOW_DAILY_UNIQUE_COUNT = "Exercise_ACT_TOGGLE_SHOW_DAILY_UNIQUE_COUNT";
    public static final String ACT_SKIP_TASK = "Exercise_ACT_SKIP_TASK";
    public static final String ACT_COPY_CARD_PATH_TO_CLIPBOARD = "Exercise_ACT_COPY_CARD_PATH_TO_CLIPBOARD";
    public static final String ACT_OPEN_CARD = "Exercise_ACT_OPEN_CARD";
    public static final String CUSTOM_EXERCISE_NAME = "CUSTOM";

    @Override
    public HtmlElem render(ExerciseState st) {
        return simplePageWithTitle("Exercise",
            form(
                switch (st) {
                    case SelectExerciseState sel -> rndSelectExerciseState(sel);
                    case RunningExerciseState run -> rndRunningExerciseState(run);
                }
            )
        );
    }

    private HtmlElem rndSelectExerciseState(SelectExerciseState st) {
        return frag(
            h4(text("Select exercise"), rndExerciseSelector(st)),
            rndCustomSelectors(st),
            rndSubmitButton(st)
        );
    }

    private HtmlElem rndSubmitButton(SelectExerciseState st) {
        if (st.makeSelectedExercise().isEmpty()) {
            return null;
        }
        return div(
            inpSubmit(ACT_START_EXERCISE, "Start")
                .attr("style", format("background-color: %s;", GREEN))
                .attr("autofocus", "").attr("class", "border-on-focus")
        );
    }

    private HtmlElem rndCustomSelectors(SelectExerciseState st) {
        if (st.getSelectedExercise().isPresent()) {
            return null;
        }
        return frag(
            rndDirSelector(st),
            rndTaskFilterSelector(st),
            rndStrategySelector(st),
            br()
        );
    }

    private HtmlElem rndExerciseSelector(SelectExerciseState st) {
        List<Pair<String, String>> allExercises = new ArrayList<>();
        allExercises.add(Pair.of(CUSTOM_EXERCISE_NAME, CUSTOM_EXERCISE_NAME));
        allExercises.addAll(st.getAllExercises().stream().map(ExerciseDef::getName).map(e -> Pair.of(e, e)).toList());
        return rndSelector(
            allExercises,
            PAR_SELECTED_EXERCISE,
            st.getSelectedExercise().map(ExerciseDef::getName),
            ""
        );
    }

    private HtmlElem rndDirSelector(SelectExerciseState st) {
        return table(List.of(List.of(
            text("Directory"),
            st.getSelectedDir().render()
        )));
    }

    private HtmlElem rndTaskFilterSelector(SelectExerciseState st) {
        return table(List.of(List.of(
            text("Task filter"),
            rndSelector(
                st.getAllTaskFilters(),
                PAR_SELECTED_TASK_FILTER,
                st.getSelectedTaskFilter().map(Pair::getLeft),
                "No task filters are defined."
            )
        )));
    }

    private HtmlElem rndStrategySelector(SelectExerciseState st) {
        return table(List.of(List.of(
            text("Repeat strategy"),
            rndSelector(
                st.getAllStrategies(),
                PAR_SELECTED_STRATEGY,
                st.getSelectedStrategy().map(Pair::getLeft),
                "No repeat strategies are defined."
            )
        )));
    }

    private <T> HtmlElem rndSelector(
        List<Pair<String, T>> elems, String paramName, Optional<String> selected, String msgOnEmpty
    ) {
        if (elems.isEmpty()) {
            return text(msgOnEmpty);
        }
        List<Pair<String, HtmlText>> options =
            elems.stream().map(Pair::getLeft).map(name -> Pair.of(name, text(name))).toList();
        return select(paramName, selected.orElse(elems.getFirst().getLeft()), options).submitOnChange();
    }

    private HtmlElem rndRunningExerciseState(RunningExerciseState st) {
        HtmlElem params;
        Optional<String> cardPath = st.getCurrentCardFile().map(File::getAbsolutePath);
        if (st.getShowMoreParams().isPresent()) {
            boolean historyUpdated = st.getTaskState().map(TaskState::isHistoryUpdated).orElse(false);
            if (st.getShowMoreParams().get()) {
                String directoriesStr = StringUtils.join(st.getDirectories(), ", ");
                String taskTypesStr = StringUtils.join(st.getTaskTypes(), ", ");
                String repeatStrategyTypesStr = StringUtils.join(st.getRepeatStrategyTypes(), ", ");
                params = frag(
                    div(text(format("Directories: %s", directoriesStr))),
                    div(text(format("Task types: %s", taskTypesStr))),
                    div(
                        text(format("Current card: %s ", cardPath.orElse("not available"))),
                        cardPath.isEmpty() ? null : frag(
                            inpSubmit(ACT_COPY_CARD_PATH_TO_CLIPBOARD, st.isCardPathCopied() ? "copied" : "copy path"),
                            inpSubmit(ACT_OPEN_CARD, "open")
                        )
                    ),
                    cardPath
                        .map(_ -> div(text(format("History updated: %s", historyUpdated ? "Yes" : "No"))))
                        .orElse(null),
                    br(),
                    div(text(format("Repeat strategies: %s", repeatStrategyTypesStr))),
                    div(st.getRepeatStrategy().renderMoreParams(historyUpdated))
                );
            } else {
                params = st.getRepeatStrategy().renderLessParams(historyUpdated);
            }
        } else {
            params = null;
        }
        HtmlElem taskContent;
        if (st.isExerciseCompleted()) {
            taskContent = frag(
                text("You have completed this exercise. "),
                inpSubmit(ACT_CANCEL_EXERCISE, "Done")
                    .attr("class", "border-on-focus").autofocus()
                    .attr("style", format("background-color: %s;", GREEN))
            );
        } else if (st.getTaskState().isPresent()) {
            taskContent = st.getTaskState().get().render();
        } else {
            taskContent = frag(
                text("There are no active tasks. "),
                inpSubmit(ACT_REFRESH_EXERCISE, "Refresh")
                    .attr("class", "border-on-focus").autofocus()
                    .attr("style", format("background-color: %s;", GREEN))
            );
        }
        return frag(
            h4(text("Exercise")),
            inpSubmit(ACT_TOGGLE_SHOW_EXERCISE_PARAMS,
                st.getShowMoreParams().isPresent() ? "Hide parameters" : "Show parameters"
            ),
            st.getShowMoreParams().map(showMoreParams ->
                inpSubmit(ACT_TOGGLE_SHOW_LESS_MORE_EXERCISE_PARAMS,
                    showMoreParams ? "Show less parameters" : "Show more parameters"
                )
            ).orElse(null),
            cardPath.isPresent() ? inpSubmit(ACT_OPEN_CARD, "Edit this card") : null,
            st.getTaskState().isPresent() ? inpSubmit(ACT_SKIP_TASK, "Skip this task") : null,
            st.getRepeatStrategy().hasDailyUniqueCount()
                ? inpSubmit(ACT_TOGGLE_SHOW_DAILY_UNIQUE_COUNT, st.isShowDailyUniqueCount() ? "Hide DUC" : "Show DUC")
                : null,
            text(rndDailyUniqueCount(st)),
            params,
            hr(),
            taskContent,
            hr(),
            st.isExerciseCompleted() ? null : inpSubmit(ACT_CANCEL_EXERCISE, "Cancel")
        );
    }

    private String rndDailyUniqueCount(RunningExerciseState st) {
        if (st.getRepeatStrategy().hasDailyUniqueCount() && st.isShowDailyUniqueCount()) {
            return st.getRepeatStrategy().getDailyUniqueCount()
                .map(duc -> String.format(" DUC: %s/%s", duc.getLeft(), duc.getRight()))
                .orElse("");
        } else {
            return "";
        }
    }
}
