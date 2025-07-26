package org.igye.remem3.app.task.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.TaskType;
import org.igye.remem3.app.impl.CardsImpl;
import org.igye.remem3.app.task.TaskResult;
import org.igye.remem3.app.task.TaskState;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.test.TestClock;
import org.igye.remem3.test.TestUtils;
import org.igye.remem3.test.impl.TestUtilsImpl;
import org.igye.remem3.utils.impl.UtilsImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.igye.remem3.app.task.impl.TaskStateTranslate.ACT_COMPLETE_TASK;
import static org.igye.remem3.app.task.impl.TaskStateTranslate.ACT_COMPLETE_TASK_WITH_MARK;
import static org.igye.remem3.app.task.impl.TaskStateTranslate.ACT_SHOW_ANS;
import static org.igye.remem3.app.task.impl.TaskStateTranslate.ACT_SUBMIT_ANSWER;
import static org.igye.remem3.app.task.impl.TaskStateTranslate.PAR_USER_ANS;

class TaskStateTranslateTest extends HtmlBuilder {
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
    void exactMatch_ansV() {
        //given
        TestClock clock = new TestClock(Instant.parse("2025-07-14T10:03:00Z"));
        Card.Translate card = Card.Translate.builder()
            .file(Optional.empty()).createdAt(Optional.empty()).history(List.of())
            .lang1("L1").text1("T1").exactMatch1(true)
            .lang2("L2").text2("T2").exactMatch2(true)
            .notes("N")
            .build();
        TaskType.Translate taskType = new TaskType.Translate(card.getLang1(), card.getLang2());
        TaskStateTranslate state = new TaskStateTranslate(clock, utils, cards, card, taskType);

        //first render
        HtmlElem html = state.render();
        assertHtmlExactMatchNoCorrectAnswer(html, "");

        //submit the correct answer
        clock.plusSeconds(10);
        TaskResult taskResult = submitAnswer("T2", html, state);
        assertTaskResult(taskResult, false,
            HistRec.builder()
                .time(Instant.parse("2025-07-14T10:03:10Z"))
                .taskType("translate:L1->L2")
                .mark(BigDecimal.ONE)
                .notes("")
                .build()
        );
        html = state.render();
        assertHtmlExactMatchHasCorrectAnswer(html, "T2");

        //click "next task" button
        clock.plusSeconds(10);
        taskResult = submitCompleteTask(html, state);
        assertTaskResult(taskResult, true, null);
    }

    @Test
    void exactMatch_ansX_ansV() {
        //given
        TestClock clock = new TestClock(Instant.parse("2025-07-14T11:03:00Z"));
        Card.Translate card = Card.Translate.builder()
            .file(Optional.empty()).createdAt(Optional.empty()).history(List.of())
            .lang1("L1").text1("T1").exactMatch1(true)
            .lang2("L2").text2("T2").exactMatch2(true)
            .notes("N")
            .build();
        TaskType.Translate taskType = new TaskType.Translate(card.getLang2(), card.getLang1());
        TaskStateTranslate state = new TaskStateTranslate(clock, utils, cards, card, taskType);

        //first render
        HtmlElem html = state.render();
        assertHtmlExactMatchNoCorrectAnswer(html, "");

        //submit an incorrect answer
        clock.plusSeconds(10);
        TaskResult taskResult = submitAnswer("T3", html, state);
        assertTaskResult(taskResult, false,
            HistRec.builder()
                .time(Instant.parse("2025-07-14T11:03:10Z"))
                .taskType("translate:L2->L1")
                .mark(BigDecimal.ZERO)
                .notes("###EXP T1 ###ACT T3")
                .build()
        );
        html = state.render();
        assertHtmlExactMatchNoCorrectAnswer(html, "T3");

        //submit the correct answer
        clock.plusSeconds(10);
        taskResult = submitAnswer("T1", html, state);
        assertTaskResult(taskResult, false, null);
        html = state.render();
        assertHtmlExactMatchHasCorrectAnswer(html, "T1");

        //click "next task" button
        clock.plusSeconds(10);
        taskResult = submitCompleteTask(html, state);
        assertTaskResult(taskResult, true, null);
    }

    @Test
    void exactMatch_showAns_ansV() {
        //given
        TestClock clock = new TestClock(Instant.parse("2025-07-14T12:03:00Z"));
        Card.Translate card = Card.Translate.builder()
            .file(Optional.empty()).createdAt(Optional.empty()).history(List.of())
            .lang1("L1").text1("T1").exactMatch1(true)
            .lang2("L2").text2("T2").exactMatch2(true)
            .notes("N")
            .build();
        TaskType.Translate taskType = new TaskType.Translate(card.getLang1(), card.getLang2());
        TaskStateTranslate state = new TaskStateTranslate(clock, utils, cards, card, taskType);

        //first render
        HtmlElem html = state.render();
        assertHtmlExactMatchNoCorrectAnswer(html, "");

        //click "show answer"
        clock.plusSeconds(10);
        TaskResult taskResult = submitShowAnswer(html, state);
        assertTaskResult(taskResult, false,
            HistRec.builder()
                .time(Instant.parse("2025-07-14T12:03:10Z"))
                .taskType("translate:L1->L2")
                .mark(BigDecimal.ZERO)
                .notes("###EXP  ###ACT <<<show_answer>>>")
                .build()
        );
        html = state.render();
        assertHtmlExactMatchNoCorrectAnswerShowAnswer(html, "");

        //submit the correct answer
        clock.plusSeconds(10);
        taskResult = submitAnswer("T2", html, state);
        assertTaskResult(taskResult, false, null);
        html = state.render();
        assertHtmlExactMatchHasCorrectAnswer(html, "T2");

        //click "next task" button
        clock.plusSeconds(10);
        taskResult = submitCompleteTask(html, state);
        assertTaskResult(taskResult, true, null);
    }

    @Test
    void exactMatch_ansX_ansX_ansV() {
        //given
        TestClock clock = new TestClock(Instant.parse("2025-07-14T11:03:00Z"));
        Card.Translate card = Card.Translate.builder()
            .file(Optional.empty()).createdAt(Optional.empty()).history(List.of())
            .lang1("L1").text1("T1").exactMatch1(true)
            .lang2("L2").text2("T2").exactMatch2(true)
            .notes("N")
            .build();
        TaskType.Translate taskType = new TaskType.Translate(card.getLang2(), card.getLang1());
        TaskStateTranslate state = new TaskStateTranslate(clock, utils, cards, card, taskType);

        //first render
        HtmlElem html = state.render();
        assertHtmlExactMatchNoCorrectAnswer(html, "");

        //submit an incorrect answer
        clock.plusSeconds(10);
        TaskResult taskResult = submitAnswer("T3", html, state);
        assertTaskResult(taskResult, false,
            HistRec.builder()
                .time(Instant.parse("2025-07-14T11:03:10Z"))
                .taskType("translate:L2->L1")
                .mark(BigDecimal.ZERO)
                .notes("###EXP T1 ###ACT T3")
                .build()
        );
        html = state.render();
        assertHtmlExactMatchNoCorrectAnswer(html, "T3");

        //submit another incorrect answer
        clock.plusSeconds(10);
        taskResult = submitAnswer("T2", html, state);
        assertTaskResult(taskResult, false, null);
        html = state.render();
        assertHtmlExactMatchNoCorrectAnswer(html, "T2");

        //submit the correct answer
        clock.plusSeconds(10);
        taskResult = submitAnswer("T1", html, state);
        assertTaskResult(taskResult, false, null);
        html = state.render();
        assertHtmlExactMatchHasCorrectAnswer(html, "T1");

        //click "next task" button
        clock.plusSeconds(10);
        taskResult = submitCompleteTask(html, state);
        assertTaskResult(taskResult, true, null);
    }

    @Test
    void exactMatch_ansX_showAns_ansV() {
        //given
        TestClock clock = new TestClock(Instant.parse("2025-07-14T12:03:00Z"));
        Card.Translate card = Card.Translate.builder()
            .file(Optional.empty()).createdAt(Optional.empty()).history(List.of())
            .lang1("L1").text1("T1").exactMatch1(true)
            .lang2("L2").text2("T2").exactMatch2(true)
            .notes("N")
            .build();
        TaskType.Translate taskType = new TaskType.Translate(card.getLang1(), card.getLang2());
        TaskStateTranslate state = new TaskStateTranslate(clock, utils, cards, card, taskType);

        //first render
        HtmlElem html = state.render();
        assertHtmlExactMatchNoCorrectAnswer(html, "");

        //submit an incorrect answer
        clock.plusSeconds(10);
        TaskResult taskResult = submitAnswer("A2", html, state);
        assertTaskResult(taskResult, false,
            HistRec.builder()
                .time(Instant.parse("2025-07-14T12:03:10Z"))
                .taskType("translate:L1->L2")
                .mark(BigDecimal.ZERO)
                .notes("###EXP T2 ###ACT A2")
                .build()
        );
        html = state.render();
        assertHtmlExactMatchNoCorrectAnswer(html, "A2");

        //click "show answer"
        clock.plusSeconds(10);
        taskResult = submitShowAnswer(html, state);
        assertTaskResult(taskResult, false, null);
        html = state.render();
        assertHtmlExactMatchNoCorrectAnswerShowAnswer(html, "A2");

        //submit the correct answer
        clock.plusSeconds(10);
        taskResult = submitAnswer("T2", html, state);
        assertTaskResult(taskResult, false, null);
        html = state.render();
        assertHtmlExactMatchHasCorrectAnswer(html, "T2");

        //click "next task" button
        clock.plusSeconds(10);
        taskResult = submitCompleteTask(html, state);
        assertTaskResult(taskResult, true, null);
    }

    @Test
    void exactMatch_showAns_ansX_ansV() {
        //given
        TestClock clock = new TestClock(Instant.parse("2025-07-14T12:03:00Z"));
        Card.Translate card = Card.Translate.builder()
            .file(Optional.empty()).createdAt(Optional.empty()).history(List.of())
            .lang1("L1").text1("T1").exactMatch1(true)
            .lang2("L2").text2("T2").exactMatch2(true)
            .notes("N")
            .build();
        TaskType.Translate taskType = new TaskType.Translate(card.getLang1(), card.getLang2());
        TaskStateTranslate state = new TaskStateTranslate(clock, utils, cards, card, taskType);

        //first render
        HtmlElem html = state.render();
        assertHtmlExactMatchNoCorrectAnswer(html, "");

        //click "show answer"
        clock.plusSeconds(10);
        TaskResult taskResult = submitShowAnswer(html, state);
        assertTaskResult(taskResult, false,
            HistRec.builder()
                .time(Instant.parse("2025-07-14T12:03:10Z"))
                .taskType("translate:L1->L2")
                .mark(BigDecimal.ZERO)
                .notes("###EXP  ###ACT <<<show_answer>>>")
                .build()
        );
        html = state.render();
        assertHtmlExactMatchNoCorrectAnswerShowAnswer(html, "");

        //submit an incorrect answer
        clock.plusSeconds(10);
        taskResult = submitAnswer("T3", html, state);
        assertTaskResult(taskResult, false, null);
        html = state.render();
        assertHtmlExactMatchNoCorrectAnswerShowAnswer(html, "T3");

        //submit the correct answer
        clock.plusSeconds(10);
        taskResult = submitAnswer("T2", html, state);
        assertTaskResult(taskResult, false, null);
        html = state.render();
        assertHtmlExactMatchHasCorrectAnswer(html, "T2");

        //click "next task" button
        clock.plusSeconds(10);
        taskResult = submitCompleteTask(html, state);
        assertTaskResult(taskResult, true, null);
    }

    @Test
    void approxMatch_ans_V() {
        //given
        TestClock clock = new TestClock(Instant.parse("2025-07-14T10:03:00Z"));
        Card.Translate card = Card.Translate.builder()
            .file(Optional.empty()).createdAt(Optional.empty()).history(List.of())
            .lang1("L1").text1("T1").exactMatch1(false)
            .lang2("L2").text2("T2").exactMatch2(false)
            .notes("N")
            .build();
        TaskType.Translate taskType = new TaskType.Translate(card.getLang1(), card.getLang2());
        TaskStateTranslate state = new TaskStateTranslate(clock, utils, cards, card, taskType);

        //first render
        HtmlElem html = state.render();
        assertHtmlApproxMatchNoAnswer(html, "");

        //submit some answer
        clock.plusSeconds(10);
        TaskResult taskResult = submitAnswer("T2", html, state);
        assertTaskResult(taskResult, false, null);
        html = state.render();
        assertHtmlApproxMatchHasAnswer(html, "T2");

        //click "answer is correct" button
        clock.plusSeconds(10);
        taskResult = submitCompleteTaskWithMark(true, html, state);
        assertTaskResult(taskResult, true,
            HistRec.builder()
                .time(Instant.parse("2025-07-14T10:03:20Z"))
                .taskType("translate:L1->L2")
                .mark(BigDecimal.ONE)
                .notes("###EXP <<<assessed_by_user>>> ###ACT T2")
                .build()
        );
    }

    @Test
    void approxMatch_ans_X() {
        //given
        TestClock clock = new TestClock(Instant.parse("2025-07-14T10:03:00Z"));
        Card.Translate card = Card.Translate.builder()
            .file(Optional.empty()).createdAt(Optional.empty()).history(List.of())
            .lang1("L1").text1("T1").exactMatch1(false)
            .lang2("L2").text2("T2").exactMatch2(false)
            .notes("N")
            .build();
        TaskType.Translate taskType = new TaskType.Translate(card.getLang1(), card.getLang2());
        TaskStateTranslate state = new TaskStateTranslate(clock, utils, cards, card, taskType);

        //first render
        HtmlElem html = state.render();
        assertHtmlApproxMatchNoAnswer(html, "");

        //submit some answer
        clock.plusSeconds(10);
        TaskResult taskResult = submitAnswer("T", html, state);
        assertTaskResult(taskResult, false, null);
        html = state.render();
        assertHtmlApproxMatchHasAnswer(html, "T");

        //click "answer is incorrect" button
        clock.plusSeconds(10);
        taskResult = submitCompleteTaskWithMark(false, html, state);
        assertTaskResult(taskResult, true,
            HistRec.builder()
                .time(Instant.parse("2025-07-14T10:03:20Z"))
                .taskType("translate:L1->L2")
                .mark(BigDecimal.ZERO)
                .notes("###EXP <<<assessed_by_user>>> ###ACT T")
                .build()
        );
    }

    @Test
    void approxMatch_showAns() {
        //given
        TestClock clock = new TestClock(Instant.parse("2025-07-14T10:03:00Z"));
        Card.Translate card = Card.Translate.builder()
            .file(Optional.empty()).createdAt(Optional.empty()).history(List.of())
            .lang1("L1").text1("T1").exactMatch1(false)
            .lang2("L2").text2("T2").exactMatch2(false)
            .notes("N")
            .build();
        TaskType.Translate taskType = new TaskType.Translate(card.getLang1(), card.getLang2());
        TaskStateTranslate state = new TaskStateTranslate(clock, utils, cards, card, taskType);

        //first render
        HtmlElem html = state.render();
        assertHtmlApproxMatchNoAnswer(html, "");

        //click "show answer" button
        clock.plusSeconds(10);
        TaskResult taskResult = submitShowAnswer(html, state);
        assertTaskResult(taskResult, false,
            HistRec.builder()
                .time(Instant.parse("2025-07-14T10:03:10Z"))
                .taskType("translate:L1->L2")
                .mark(BigDecimal.ZERO)
                .notes("###EXP  ###ACT <<<show_answer>>>")
                .build()
        );
        html = state.render();
        assertHtmlApproxMatchShowAnswer(html, "");

        //click "next task" button
        clock.plusSeconds(10);
        taskResult = submitCompleteTask(html, state);
        assertTaskResult(taskResult, true, null);
    }

    private void assertHtmlExactMatchNoCorrectAnswer(HtmlElem html, String userAns) {
        testUtils.assertInputs(html,
            inpText(PAR_USER_ANS, userAns, ACT_SUBMIT_ANSWER, true)
                .attr("size", "200").attr("spellcheck", "false").attr("class", "border-on-focus"),
            inpSubmit(ACT_SUBMIT_ANSWER, "Submit answer"),
            inpSubmit(ACT_SHOW_ANS, "Show answer").attr("style", "background-color: orange;")
        );
    }

    private void assertHtmlExactMatchNoCorrectAnswerShowAnswer(HtmlElem html, String userAns) {
        testUtils.assertInputs(html,
            inpText(PAR_USER_ANS, userAns, ACT_SUBMIT_ANSWER, true)
                .attr("size", "200").attr("spellcheck", "false").attr("class", "border-on-focus"),
            inpSubmit(ACT_SUBMIT_ANSWER, "Submit answer")
        );
    }

    private void assertHtmlExactMatchHasCorrectAnswer(HtmlElem html, String userAns) {
        testUtils.assertInputs(html,
            inpText(PAR_USER_ANS, userAns, ACT_SUBMIT_ANSWER, true).attr("size", "200")
                .attr("disabled", "").attr("spellcheck", "false").attr("class", "border-on-focus"),
            inpHidden(PAR_USER_ANS, userAns),
            inpSubmit(ACT_COMPLETE_TASK, "Next task").attr("style", "background-color: green;"),
            inpText("", "", ACT_COMPLETE_TASK, true)
                .attr("size", "1").attr("class", "border-on-focus")
        );
    }

    private void assertHtmlApproxMatchNoAnswer(HtmlElem html, String userAns) {
        testUtils.assertInputs(html,
            inpText(PAR_USER_ANS, userAns, ACT_SUBMIT_ANSWER, true)
                .attr("size", "200").attr("spellcheck", "false").attr("class", "border-on-focus"),
            inpSubmit(ACT_SUBMIT_ANSWER, "Submit answer"),
            inpSubmit(ACT_SHOW_ANS, "Show answer").attr("style", "background-color: orange;")
        );
    }

    private void assertHtmlApproxMatchHasAnswer(HtmlElem html, String userAns) {
        testUtils.assertInputs(html,
            inpText(PAR_USER_ANS, userAns, ACT_SUBMIT_ANSWER, true)
                .attr("size", "200").attr("disabled", "").attr("spellcheck", "false").attr("class", "border-on-focus"),
            inpHidden(PAR_USER_ANS, userAns),
            inpSubmit(ACT_COMPLETE_TASK_WITH_MARK + ":0", "Incorrect").attr("style", "background-color: red;"),
            inpSubmit(ACT_COMPLETE_TASK_WITH_MARK + ":1", "Correct").attr("style", "background-color: green;"),
            inpText("", "", ACT_COMPLETE_TASK_WITH_MARK + ":0", true)
                .attr("size", "1").attr("class", "border-on-focus"),
            inpText("", "", ACT_COMPLETE_TASK_WITH_MARK + ":1")
                .attr("size", "1").attr("class", "border-on-focus")
        );
    }

    private void assertHtmlApproxMatchShowAnswer(HtmlElem html, String userAns) {
        testUtils.assertInputs(html,
            inpText(PAR_USER_ANS, userAns, ACT_SUBMIT_ANSWER, true)
                .attr("size", "200").attr("disabled", "").attr("spellcheck", "false").attr("class", "border-on-focus"),
            inpHidden(PAR_USER_ANS, userAns),
            inpSubmit(ACT_COMPLETE_TASK, "Next task").attr("style", "background-color: green;"),
            inpText("", "", ACT_COMPLETE_TASK, true).attr("size", "1").attr("class", "border-on-focus")
        );
    }

    private TaskResult submitAnswer(String answer, HtmlElem html, TaskState state) {
        testUtils.setValue(html, PAR_USER_ANS, answer);
        return state.processUserInput(testUtils.submit(html, ACT_SUBMIT_ANSWER));
    }

    private TaskResult submitShowAnswer(HtmlElem html, TaskState state) {
        return state.processUserInput(testUtils.submit(html, ACT_SHOW_ANS));
    }

    private TaskResult submitCompleteTask(HtmlElem html, TaskState state) {
        return state.processUserInput(testUtils.submit(html, ACT_COMPLETE_TASK));
    }

    private TaskResult submitCompleteTaskWithMark(boolean ansIsCorrect, HtmlElem html, TaskState state) {
        return state.processUserInput(
            testUtils.submit(html, ACT_COMPLETE_TASK_WITH_MARK + (ansIsCorrect ? ":1" : ":0"))
        );
    }

    private void assertTaskResult(TaskResult result, boolean completed, HistRec histRec) {
        Assertions.assertEquals(completed, result.isCompleted());
        Assertions.assertEquals(Optional.ofNullable(histRec), result.getHistRec());
    }
}