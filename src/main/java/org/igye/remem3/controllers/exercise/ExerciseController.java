package org.igye.remem3.controllers.exercise;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.App;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.dto.TaskType;
import org.igye.remem3.app.impl.CacheImpl;
import org.igye.remem3.app.impl.CardsImpl;
import org.igye.remem3.app.impl.SettingsImpl;
import org.igye.remem3.controllers.components.DirSelectorCmp;
import org.igye.remem3.controllers.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.controllers.components.impl.RepeatStrategyCmpImpl;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.utils.web.RequestParams;
import org.igye.remem3.utils.web.impl.RequestParamsImpl;
import org.igye.remem3.web.StatefulWebController;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ExerciseController extends HtmlBuilder
    implements StatefulWebController<ExerciseState, Supplier<ExerciseState>> {

    private static final String PAR_DIR_TO_READ_TASKS_FROM = "PAR_DIR_TO_READ_TASKS_FROM";
    private static final String PAR_TASK_TYPE = "PAR_TASK_TYPE";

    private final App app;
    private final Utils utils;

    @Override
    public String getPath() {
        return "exercise";
    }

    @Override
    public ExerciseState loadState(HttpServletRequest req) {
        try {
            app.reloadProperties();
            Settings settings = SettingsImpl.load(app);
            Cache cache = CacheImpl.load(utils, settings);
            RequestParams params = new RequestParamsImpl(req);
            DirSelectorCmp dirSelector = new DirSelectorCmpImpl(settings, cache, params, PAR_DIR_TO_READ_TASKS_FROM);
            CardsImpl cards = new CardsImpl(utils, settings);
            List<TaskType> availableTaskTypes = cards.loadAllCards(dirSelector.getSelectedDirectory()).stream()
                .flatMap(card -> card.getTaskTypes().stream())
                .distinct()
                .sorted(Comparator.comparing(TaskType::getCode))
                .toList();
            return ExerciseState.builder()
                .cards(cards)
                .dirSelector(dirSelector)
                .taskTypes(getTaskTypes(availableTaskTypes, params))
                .repeatStrategyCmp(new RepeatStrategyCmpImpl("PAR_REPEAT_STRATEGY", params))
                .build();
        } catch (Exception ex) {
            return ExerciseState.builder()
                .errors(List.of(ex.getMessage()))
                .build();
        }
    }

    @Override
    public Optional<Supplier<ExerciseState>> decodeAction(HttpServletRequest req, ExerciseState state) {
        if (CollectionUtils.isNotEmpty(state.getErrors())) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    @Override
    public ExerciseState updateState(ExerciseState state, Supplier<ExerciseState> action) {
        if (CollectionUtils.isNotEmpty(state.getErrors())) {
            return state;
        }
        return action.get();
    }

    @Override
    public void saveState(ExerciseState state) {

    }

    @Override
    public String renderState(ExerciseState st) {
        return simplePageWithTitle("Exercise",
            form(
                rndDirSelector(st),
                rndTaskTypes(st),
                h("br"),
                st.getRepeatStrategyCmp().render()
            )
        ).toString();
    }

    private List<Pair<TaskType, Boolean>> getTaskTypes(List<TaskType> availableTaskTypes, RequestParams params) {
        Set<String> checked = Arrays.stream(params.getParams(PAR_TASK_TYPE)).collect(Collectors.toSet());
        return availableTaskTypes.stream()
            .map(typ -> Pair.of(typ, checked.contains(typ.getCode())))
            .toList();
    }

    private HtmlElem rndTaskTypes(ExerciseState st) {
        return frag(
            h5(text("Task types")),
            table(
                st.getTaskTypes().stream()
                    .map(typ -> List.of(
                        inpCheckbox(PAR_TASK_TYPE, typ.getLeft().getCode(), typ.getRight()),
                        text(typ.getLeft().getCode())
                    ))
                    .toList()
            )
        );
    }

    private HtmlTag rndDirSelector(ExerciseState st) {
        return table(List.of(List.of(
            text("Directory"),
            frag(st.getDirSelector().render())
        )));
    }
}
