package org.igye.remem3.app.taskstate.impl;

import org.igye.remem3.app.Settings;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.TaskType;
import org.igye.remem3.app.dto.fillgaps.TextPart;
import org.igye.remem3.app.impl.CardUtilsImpl;
import org.igye.remem3.app.taskstate.TaskResult;
import org.igye.remem3.app.taskstate.TaskState;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.test.TestClock;
import org.igye.remem3.test.TestUtils;
import org.igye.remem3.test.impl.TestUtilsImpl;
import org.igye.remem3.utils.impl.UtilsImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static org.igye.remem3.app.taskstate.impl.TaskStateFillGaps.ACT_COMPLETE_TASK;
import static org.igye.remem3.app.taskstate.impl.TaskStateFillGaps.ACT_SHOW_ANS;
import static org.igye.remem3.app.taskstate.impl.TaskStateFillGaps.ACT_SHOW_HINT;
import static org.igye.remem3.app.taskstate.impl.TaskStateFillGaps.ACT_SUBMIT_ANSWERS;
import static org.igye.remem3.app.taskstate.impl.TaskStateTranslate.PAR_USER_ANS;


class TaskStateFillGapsTest extends HtmlBuilder {
    private final TestUtils testUtils = new TestUtilsImpl();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UtilsImpl utils = new UtilsImpl(objectMapper);
    private final Settings settings = Mockito.mock(Settings.class);
    private final CardUtilsImpl cards = new CardUtilsImpl(utils, settings);

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
        TaskResult taskResult = submitAnswer(List.of("bb"), html, state);
        assertTaskResult(taskResult, false,
            HistRec.builder()
                .time(Instant.parse("2025-07-15T10:03:10Z"))
                .taskType("fill_gaps:L1")
                .mark(BigDecimal.ZERO)
                .notes("###EXP B ###ACT bb")
                .build()
        );
        html = state.render();
        assertHtmlNoCorrectAnswer(html, List.of("B"), List.of("bb"));

        //submit another incorrect answer
        clock.plusSeconds(10);
        taskResult = submitAnswer(List.of("b"), html, state);
        assertTaskResult(taskResult, false, null);
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
    void gap1_ansX_showHint_ansV() {
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
        TaskResult taskResult = submitAnswer(List.of("..."), html, state);
        assertTaskResult(taskResult, false,
            HistRec.builder()
                .time(Instant.parse("2025-07-15T10:03:10Z"))
                .taskType("fill_gaps:L1")
                .mark(BigDecimal.ZERO)
                .notes("###EXP B ###ACT ...")
                .build()
        );
        html = state.render();
        assertHtmlNoCorrectAnswer(html, List.of("B"), List.of("..."));

        //show hint
        clock.plusSeconds(10);
        taskResult = submitShowHint(html, state);
        assertTaskResult(taskResult, false, null);
        html = state.render();
        assertHtmlNoCorrectAnswerShowHint(html, List.of("B"), List.of("..."));

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
    void gap1_ansX_showAnswer_ansV() {
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
        TaskResult taskResult = submitAnswer(List.of("..."), html, state);
        assertTaskResult(taskResult, false,
            HistRec.builder()
                .time(Instant.parse("2025-07-15T10:03:10Z"))
                .taskType("fill_gaps:L1")
                .mark(BigDecimal.ZERO)
                .notes("###EXP B ###ACT ...")
                .build()
        );
        html = state.render();
        assertHtmlNoCorrectAnswer(html, List.of("B"), List.of("..."));

        //show answer
        clock.plusSeconds(10);
        taskResult = submitShowAnswer(html, state);
        assertTaskResult(taskResult, false, null);
        html = state.render();
        assertHtmlNoCorrectAnswerShowAnswer(html, List.of("B"), List.of("..."));

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
    void gap1_showHint_ansV() {
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

        //show hint
        clock.plusSeconds(10);
        TaskResult taskResult = submitShowHint(html, state);
        assertTaskResult(taskResult, false,
            HistRec.builder()
                .time(Instant.parse("2025-07-15T10:03:10Z"))
                .taskType("fill_gaps:L1")
                .mark(BigDecimal.ZERO)
                .notes("###EXP B ###ACT")
                .build()
        );
        html = state.render();
        assertHtmlNoCorrectAnswerShowHint(html, List.of("B"), List.of(""));

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
    void gap1_showHint_ansX_ansV() {
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

        //show hint
        clock.plusSeconds(10);
        TaskResult taskResult = submitShowHint(html, state);
        assertTaskResult(taskResult, false,
            HistRec.builder()
                .time(Instant.parse("2025-07-15T10:03:10Z"))
                .taskType("fill_gaps:L1")
                .mark(BigDecimal.ZERO)
                .notes("###EXP B ###ACT")
                .build()
        );
        html = state.render();
        assertHtmlNoCorrectAnswerShowHint(html, List.of("B"), List.of(""));

        //submit an incorrect answer
        clock.plusSeconds(10);
        taskResult = submitAnswer(List.of("."), html, state);
        assertTaskResult(taskResult, false, null);
        html = state.render();
        assertHtmlNoCorrectAnswerShowHint(html, List.of("B"), List.of("."));

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
    void gap1_showAns_ansX_ansV() {
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

        //show answer
        clock.plusSeconds(10);
        TaskResult taskResult = submitShowAnswer(html, state);
        assertTaskResult(taskResult, false,
            HistRec.builder()
                .time(Instant.parse("2025-07-15T10:03:10Z"))
                .taskType("fill_gaps:L1")
                .mark(BigDecimal.ZERO)
                .notes("###EXP B ###ACT")
                .build()
        );
        html = state.render();
        assertHtmlNoCorrectAnswerShowAnswer(html, List.of("B"), List.of(""));

        //submit an incorrect answer
        clock.plusSeconds(10);
        taskResult = submitAnswer(List.of("."), html, state);
        assertTaskResult(taskResult, false, null);
        html = state.render();
        assertHtmlNoCorrectAnswerShowAnswer(html, List.of("B"), List.of("."));

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
    void gap1_showAns_ansV() {
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

        //show answer
        clock.plusSeconds(10);
        TaskResult taskResult = submitShowAnswer(html, state);
        assertTaskResult(taskResult, false,
            HistRec.builder()
                .time(Instant.parse("2025-07-15T10:03:10Z"))
                .taskType("fill_gaps:L1")
                .mark(BigDecimal.ZERO)
                .notes("###EXP B ###ACT")
                .build()
        );
        html = state.render();
        assertHtmlNoCorrectAnswerShowAnswer(html, List.of("B"), List.of(""));

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
    void gap3_ansX1_ansX2_ansV() {
        //given
        TestClock clock = new TestClock(Instant.parse("2025-07-15T10:03:00Z"));
        Card.FillGaps card = Card.FillGaps.builder()
            .file(Optional.empty()).createdAt(Optional.empty()).history(List.of())
            .lang("L1").descr("DDD").text(List.of(
                TextPart.Text.builder().text("A").build(),
                TextPart.Gap.builder().answer("B").hint("b").notes("/b/").build(),
                TextPart.Text.builder().text("C").build(),
                TextPart.Gap.builder().answer("D").hint("d").notes("/d/").build(),
                TextPart.Text.builder().text("E").build(),
                TextPart.Gap.builder().answer("F").hint("f").notes("/f/").build(),
                TextPart.Text.builder().text("G").build()
            ))
            .notes("N")
            .build();
        TaskType.FillGaps taskType = new TaskType.FillGaps(card.getLang());
        TaskStateFillGaps state = new TaskStateFillGaps(clock, utils, cards, card, taskType);

        //first render
        HtmlElem html = state.render();
        assertHtmlNoCorrectAnswer(html, List.of("B", "D", "F"), List.of("", "", ""));

        //submit an incorrect answer 1
        clock.plusSeconds(10);
        TaskResult taskResult = submitAnswer(List.of("b", "D", "f"), html, state);
        assertTaskResult(taskResult, false,
            HistRec.builder()
                .time(Instant.parse("2025-07-15T10:03:10Z"))
                .taskType("fill_gaps:L1")
                .mark(BigDecimal.ZERO)
                .notes("###EXP B ###ACT b ###EXP F ###ACT f")
                .build()
        );
        html = state.render();
        assertHtmlNoCorrectAnswer(html, List.of("B", "D", "F"), List.of("b", "D", "f"));

        //submit an incorrect answer 2
        clock.plusSeconds(10);
        taskResult = submitAnswer(List.of("B", "D", "f"), html, state);
        assertTaskResult(taskResult, false, null);
        html = state.render();
        assertHtmlNoCorrectAnswer(html, List.of("B", "D", "F"), List.of("B", "D", "f"));

        //submit the correct answer
        clock.plusSeconds(10);
        taskResult = submitAnswer(List.of("B", "D", "F"), html, state);
        assertTaskResult(taskResult, false, null);
        html = state.render();
        assertHtmlHasCorrectAnswer(html, List.of("B", "D", "F"));

        //click the "next task" button
        clock.plusSeconds(10);
        taskResult = submitCompleteTask(html, state);
        assertTaskResult(taskResult, true, null);
    }

    private List<HtmlTag> makeAnsElems(List<String> expAns, List<String> userAns) {
        List<HtmlTag> elems = new ArrayList<>();
        for (int i = 0; i < userAns.size(); i++) {
            String uAns = userAns.get(i);
            String eAns = expAns.get(i);
            if (eAns.equals(uAns)) {
                elems.add(
                    inpText(PAR_USER_ANS + ":" + i, uAns, ACT_SUBMIT_ANSWERS).autofocus()
                        .attr("size", "20").attr("disabled", "")
                        .attr("spellcheck", "false").attr("class", "border-on-focus")
                );
                elems.add(inpHidden(PAR_USER_ANS + ":" + i, uAns));
            } else {
                elems.add(
                    inpText(PAR_USER_ANS + ":" + i, uAns, ACT_SUBMIT_ANSWERS).autofocus()
                        .attr("size", "20").attr("spellcheck", "false").attr("class", "border-on-focus")
                );
            }
        }
        return elems;
    }

    private void assertHtmlNoCorrectAnswer(HtmlElem html, List<String> expAns, List<String> userAns) {
        List<HtmlTag> elems = new ArrayList<>(makeAnsElems(expAns, userAns));
        elems.add(inpSubmit(ACT_SUBMIT_ANSWERS, "Submit answer").attr("class", "border-on-focus"));
        elems.add(
            inpSubmit(ACT_SHOW_HINT, "Hint")
                .attr("style", format("background-color: %s;", ORANGE)).attr("class", "border-on-focus")
        );
        elems.add(
            inpSubmit(ACT_SHOW_ANS, "Show answer")
                .attr("style", format("background-color: %s;", ORANGE)).attr("class", "border-on-focus")
        );
        testUtils.assertInputs(html, elems);
    }

    private void assertHtmlNoCorrectAnswerShowHint(HtmlElem html, List<String> expAns, List<String> userAns) {
        List<HtmlTag> elems = new ArrayList<>(makeAnsElems(expAns, userAns));
        elems.add(
            inpSubmit(ACT_SUBMIT_ANSWERS, "Submit answer").attr("class", "border-on-focus")
        );
        elems.add(
            inpSubmit(ACT_SHOW_ANS, "Show answer")
                .attr("style", format("background-color: %s;", ORANGE)).attr("class", "border-on-focus")
        );
        testUtils.assertInputs(html, elems);
    }

    private void assertHtmlNoCorrectAnswerShowAnswer(HtmlElem html, List<String> expAns, List<String> userAns) {
        List<HtmlTag> elems = new ArrayList<>(makeAnsElems(expAns, userAns));
        elems.add(inpSubmit(ACT_SUBMIT_ANSWERS, "Submit answer").attr("class", "border-on-focus"));
        testUtils.assertInputs(html, elems);
    }

    private void assertHtmlHasCorrectAnswer(HtmlElem html, List<String> expAns) {
        List<HtmlTag> elems = new ArrayList<>(makeAnsElems(expAns, expAns));
        elems.add(
            inpSubmit(ACT_COMPLETE_TASK, "Next task")
                .attr("style", format("background-color: %s;", GREEN)).attr("class", "border-on-focus")
                .attr("autofocus", "")
        );
        testUtils.assertInputs(html, elems);
    }

    private TaskResult submitAnswer(List<String> userAns, HtmlElem html, TaskState state) {
        for (int i = 0; i < userAns.size(); i++) {
            String paramName = PAR_USER_ANS + ":" + i;
            if (testUtils.isWritable(html, paramName)) {
                testUtils.setValue(html, paramName, userAns.get(i));
            }
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