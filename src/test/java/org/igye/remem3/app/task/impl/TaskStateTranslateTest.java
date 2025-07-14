package org.igye.remem3.app.task.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.TaskType;
import org.igye.remem3.app.impl.CardsImpl;
import org.igye.remem3.app.task.TaskResult;
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

        //when - first render
        HtmlElem html = state.render();

        //then
        testUtils.assertInputs(html,
            inpText(PAR_USER_ANS, "", ACT_SUBMIT_ANSWER).attr("size", "200"),
            inpSubmit(ACT_SUBMIT_ANSWER, "Submit answer"),
            inpSubmit(ACT_SHOW_ANS, "Show answer").attr("style", "background-color: orange;")
        );

        //when - submit the correct answer
        testUtils.setValue(html, PAR_USER_ANS, "T2");
        clock.plusSeconds(10);
        TaskResult taskResult = state.processUserInput(testUtils.submit(html, ACT_SUBMIT_ANSWER));

        //then
        Assertions.assertEquals(
            Optional.of(
                HistRec.builder()
                    .time(Instant.parse("2025-07-14T10:03:10Z"))
                    .taskType("translate:L1->L2")
                    .mark(BigDecimal.ONE)
                    .notes("")
                    .build()
            ),
            taskResult.getHistRec()
        );
        Assertions.assertFalse(taskResult.isCompleted());

        //when - "next task" button
        html = state.render();

        //then
        testUtils.assertInputs(html,
            inpText(PAR_USER_ANS, "T2", ACT_SUBMIT_ANSWER).attr("size", "200").attr("disabled", ""),
            inpHidden(PAR_USER_ANS, "T2"),
            inpSubmit(ACT_COMPLETE_TASK, "Next task").attr("style", "background-color: green;"),
            inpText("", "", ACT_COMPLETE_TASK).attr("size", "1")
        );

        //when - press "next task"
        clock.plusSeconds(10);
        taskResult = state.processUserInput(testUtils.submit(html, ACT_COMPLETE_TASK));

        //then
        Assertions.assertEquals(Optional.empty(), taskResult.getHistRec());
        Assertions.assertTrue(taskResult.isCompleted());
    }

}