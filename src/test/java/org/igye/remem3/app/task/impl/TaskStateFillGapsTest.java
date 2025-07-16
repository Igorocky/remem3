package org.igye.remem3.app.task.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.TaskType;
import org.igye.remem3.app.dto.fillgaps.TextPart;
import org.igye.remem3.app.impl.CardsImpl;
import org.igye.remem3.app.task.TaskResult;
import org.igye.remem3.app.task.TaskState;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.test.TestClock;
import org.igye.remem3.test.TestUtils;
import org.igye.remem3.test.impl.TestUtilsImpl;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.impl.UtilsImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.igye.remem3.app.task.impl.TaskStateFillGaps.ACT_COMPLETE_TASK;
import static org.igye.remem3.app.task.impl.TaskStateFillGaps.ACT_SHOW_ANS;
import static org.igye.remem3.app.task.impl.TaskStateFillGaps.ACT_SHOW_HINT;
import static org.igye.remem3.app.task.impl.TaskStateFillGaps.ACT_SUBMIT_ANSWERS;
import static org.igye.remem3.app.task.impl.TaskStateTranslate.PAR_USER_ANS;


class TaskStateFillGapsTest extends HtmlBuilder {
    private final TestUtils testUtils = new TestUtilsImpl();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UtilsImpl utils = new UtilsImpl(objectMapper);
    private final Settings settings = Mockito.mock(Settings.class);
    private final CardsImpl cards = new CardsImpl(utils, settings);

    @BeforeEach
    void setup() {
        Mockito.reset(settings);
        Mockito.when(settings.getLanguages()).thenReturn(List.of("L1", "L2"));
    }

    @Test
    void gap1_ansV() {
        //given
        TestClock clock = new TestClock(Instant.parse("2025-07-15T10:03:00Z"));
        Card.FillGaps card = Card.FillGaps.builder()
            .file(Optional.empty()).createdAt(Optional.empty()).history(List.of())
            .lang("L1").descr("DDD").text(List.of(
                TextPart.Text.builder().text("A").build(),
                TextPart.Gap.builder().answer("B").hint("b").notes("/b/").build(),
                TextPart.Text.builder().text("C").build()
            ))
            .notes("N")
            .build();
        TaskType.FillGaps taskType = new TaskType.FillGaps(card.getLang());
        TaskStateFillGaps state = new TaskStateFillGaps(clock, utils, cards, card, taskType);

        //first render
        HtmlElem html = state.render();
        assertHtmlNoCorrectAnswer(html, List.of("B"), List.of(""));

        //submit the correct answer
        clock.plusSeconds(10);
        TaskResult taskResult = submitAnswer(List.of("B"), html, state);
        assertTaskResult(taskResult, false,
            HistRec.builder()
                .time(Instant.parse("2025-07-15T10:03:10Z"))
                .taskType("fill_gaps:L1")
                .mark(BigDecimal.ONE)
                .notes("")
                .build()
        );
        html = state.render();
        assertHtmlHasCorrectAnswer(html, List.of("B"));

        //click the "next task" button
        clock.plusSeconds(10);
        taskResult = submitCompleteTask(html, state);
        assertTaskResult(taskResult, true, null);
    }

    @Test
    void gap1_ansX_ansV() {
        //given
        TestClock clock = new TestClock(Instant.parse("2025-07-15T10:03:00Z"));
        Card.FillGaps card = Card.FillGaps.builder()
            .file(Optional.empty()).createdAt(Optional.empty()).history(List.of())
            .lang("L1").descr("DDD").text(List.of(
                TextPart.Text.builder().text("A").build(),
                TextPart.Gap.builder().answer("B").hint("b").notes("/b/").build(),
                TextPart.Text.builder().text("C").build()
            ))
            .notes("N")
            .build();
        TaskType.FillGaps taskType = new TaskType.FillGaps(card.getLang());
        TaskStateFillGaps state = new TaskStateFillGaps(clock, utils, cards, card, taskType);

        //first render
        HtmlElem html = state.render();
        assertHtmlNoCorrectAnswer(html, List.of("B"), List.of(""));

        //submit an incorrect answer
        clock.plusSeconds(10);
        TaskResult taskResult = submitAnswer(List.of("b"), html, state);
        assertTaskResult(taskResult, false,
            HistRec.builder()
                .time(Instant.parse("2025-07-15T10:03:10Z"))
                .taskType("fill_gaps:L1")
                .mark(BigDecimal.ZERO)
                .notes("###EXP B ###ACT b")
                .build()
        );
        html = state.render();
        assertHtmlNoCorrectAnswer(html, List.of("B"), List.of("b"));

        //submit the correct answer
        clock.plusSeconds(10);
        taskResult = submitAnswer(List.of("B"), html, state);
        assertTaskResult(taskResult, false, null);
        html = state.render();
        assertHtmlHasCorrectAnswer(html, List.of("B"));

        //click the "next task" button
        clock.plusSeconds(10);
        taskResult = submitCompleteTask(html, state);
        assertTaskResult(taskResult, true, null);
    }

    @Test
    void gap1_ansX_ansX_ansV() {
        throw new Exn("not implemented");
    }

    @Test
    void gap1_showHint_ansV() {
        throw new Exn("not implemented");
    }

    @Test
    void gap1_showAnswer_ansV() {
        throw new Exn("not implemented");
    }

    private List<HtmlTag> makeAnsElems(List<String> expAns, List<String> userAns) {
        List<HtmlTag> elems = new ArrayList<>();
        for (int i = 0; i < userAns.size(); i++) {
            String uAns = userAns.get(i);
            String eAns = expAns.get(i);
            if (eAns.equals(uAns)) {
                elems.add(
                    inpText(PAR_USER_ANS + ":" + i, uAns, ACT_SUBMIT_ANSWERS).attr("size", "20").attr("disabled", "")
                );
                elems.add(inpHidden(PAR_USER_ANS + ":" + i, uAns));
            } else {
                elems.add(inpText(PAR_USER_ANS + ":" + i, uAns, ACT_SUBMIT_ANSWERS).attr("size", "20"));
            }
        }
        return elems;
    }

    private void assertHtmlNoCorrectAnswer(HtmlElem html, List<String> expAns, List<String> userAns) {
        List<HtmlTag> elems = new ArrayList<>(makeAnsElems(expAns, userAns));
        elems.add(inpSubmit(ACT_SUBMIT_ANSWERS, "Submit answer"));
        elems.add(inpSubmit(ACT_SHOW_HINT, "Hint").attr("style", "background-color: orange;"));
        elems.add(inpSubmit(ACT_SHOW_ANS, "Show answer").attr("style", "background-color: orange;"));
        testUtils.assertInputs(html, elems);
    }

    private void assertHtmlNoCorrectAnswerShowHint(HtmlElem html, List<String> expAns, List<String> userAns) {
        List<HtmlTag> elems = new ArrayList<>(makeAnsElems(expAns, userAns));
        elems.add(inpSubmit(ACT_SUBMIT_ANSWERS, "Submit answer"));
        elems.add(inpSubmit(ACT_SHOW_ANS, "Show answer").attr("style", "background-color: orange;"));
        testUtils.assertInputs(html, elems);
    }

    private void assertHtmlNoCorrectAnswerShowAnswer(HtmlElem html, List<String> expAns, List<String> userAns) {
        List<HtmlTag> elems = new ArrayList<>(makeAnsElems(expAns, userAns));
        elems.add(inpSubmit(ACT_SUBMIT_ANSWERS, "Submit answer"));
        testUtils.assertInputs(html, elems);
    }

    private void assertHtmlHasCorrectAnswer(HtmlElem html, List<String> expAns) {
        List<HtmlTag> elems = new ArrayList<>(makeAnsElems(expAns, expAns));
        elems.add(inpSubmit(ACT_COMPLETE_TASK, "Next task").attr("style", "background-color: green;"));
        elems.add(inpText("", "", ACT_COMPLETE_TASK).attr("size", "1"));
        testUtils.assertInputs(html, elems);
    }

    private TaskResult submitAnswer(List<String> userAns, HtmlElem html, TaskState state) {
        for (int i = 0; i < userAns.size(); i++) {
            testUtils.setValue(html, PAR_USER_ANS + ":" + i, userAns.get(i));
        }
        return state.processUserInput(testUtils.submit(html, ACT_SUBMIT_ANSWERS));
    }

    private TaskResult submitShowHint(HtmlElem html, TaskState state) {
        return state.processUserInput(testUtils.submit(html, ACT_SHOW_HINT));
    }

    private TaskResult submitShowAnswer(HtmlElem html, TaskState state) {
        return state.processUserInput(testUtils.submit(html, ACT_SHOW_ANS));
    }

    private TaskResult submitCompleteTask(HtmlElem html, TaskState state) {
        return state.processUserInput(testUtils.submit(html, ACT_COMPLETE_TASK));
    }

    private void assertTaskResult(TaskResult result, boolean completed, HistRec histRec) {
        Assertions.assertEquals(completed, result.isCompleted());
        Assertions.assertEquals(Optional.ofNullable(histRec), result.getHistRec());
    }
}