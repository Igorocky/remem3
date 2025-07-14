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
    void exact_match_first_ans_is_correct() {
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

    private void assertHtmlExactMatchNoCorrectAnswer(HtmlElem html, String userAns) {
        testUtils.assertInputs(html,
            inpText(PAR_USER_ANS, userAns, ACT_SUBMIT_ANSWER).attr("size", "200"),
            inpSubmit(ACT_SUBMIT_ANSWER, "Submit answer"),
            inpSubmit(ACT_SHOW_ANS, "Show answer").attr("style", "background-color: orange;")
        );
    }

    private void assertHtmlExactMatchHasCorrectAnswer(HtmlElem html, String userAns) {
        testUtils.assertInputs(html,
            inpText(PAR_USER_ANS, userAns, ACT_SUBMIT_ANSWER).attr("size", "200").attr("disabled", ""),
            inpHidden(PAR_USER_ANS, userAns),
            inpSubmit(ACT_COMPLETE_TASK, "Next task").attr("style", "background-color: green;"),
            inpText("", "", ACT_COMPLETE_TASK).attr("size", "1")
        );
    }

    private TaskResult submitAnswer(String answer, HtmlElem html, TaskState state) {
        testUtils.setValue(html, PAR_USER_ANS, answer);
        return state.processUserInput(testUtils.submit(html, ACT_SUBMIT_ANSWER));
    }

    private TaskResult submitCompleteTask(HtmlElem html, TaskState state) {
        return state.processUserInput(testUtils.submit(html, ACT_COMPLETE_TASK));
    }

    private void assertTaskResult(TaskResult result, boolean completed, HistRec histRec) {
        Assertions.assertEquals(completed, result.isCompleted());
        Assertions.assertEquals(
            histRec == null ? Optional.empty() : Optional.of(histRec),
            result.getHistRec()
        );
    }
}