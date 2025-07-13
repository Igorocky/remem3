package org.igye.remem3.app.task.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.igye.remem3.app.Cards;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.TaskType;
import org.igye.remem3.app.task.TaskResult;
import org.igye.remem3.app.task.TaskState;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.web.RequestParams;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskStateTranslate extends HtmlBuilder implements TaskState {
    protected static final String PAR_USER_ANS = "PAR_USER_ANS";
    protected static final String ACT_SUBMIT_ANSWER = "ACT_SUBMIT_ANSWER";
    protected static final String ACT_SHOW_ANS = "ACT_SHOW_ANS";
    protected static final String ACT_COMPLETE_TASK = "ACT_COMPLETE_TASK";
    protected static final String ACT_COMPLETE_TASK_WITH_MARK = "ACT_COMPLETE_TASK_WITH_MARK";

    private final Clock clock;
    private final Utils utils;
    private final Card.Translate card;
    private final TaskType.Translate taskType;
    private final List<String> cardErrors;
    private final String textToTranslate;
    private final String expAnswer;
    private final boolean exactMatch;

    private String userAnswer;
    private Optional<Boolean> userAnswerIsCorrect = Optional.empty();
    private Optional<HistRec> histRec = Optional.empty();
    private boolean showAnswer;

    public TaskStateTranslate(Clock clock, Utils utils, Cards cards, Card.Translate card, TaskType.Translate taskType) {
        this.clock = clock;
        this.utils = utils;
        this.card = card;
        this.taskType = taskType;
        cardErrors = new ArrayList<>(cards.validateCard(card));
        if (CollectionUtils.isNotEmpty(cardErrors)) {
            textToTranslate = null;
            expAnswer = null;
            exactMatch = false;
            return;
        }
        if (card.getLang1().equals(taskType.getLangFrom()) && card.getLang2().equals(taskType.getLangTo())) {
            textToTranslate = card.getText1();
            expAnswer = card.getText2();
            exactMatch = card.isExactMatch2();
        } else if (card.getLang2().equals(taskType.getLangFrom()) && card.getLang1().equals(taskType.getLangTo())) {
            textToTranslate = card.getText2();
            expAnswer = card.getText1();
            exactMatch = card.isExactMatch1();
        } else {
            cardErrors.add(String.format(
                "Languages from the task %s->%s don't match languages in the card %s<->%s.",
                taskType.getLangFrom(), taskType.getLangTo(), card.getLang1(), card.getLang2()
            ));
            textToTranslate = null;
            expAnswer = null;
            exactMatch = false;
        }
    }

    @Override
    public TaskResult processUserInput(RequestParams params) {
        if (CollectionUtils.isNotEmpty(cardErrors)) {
            return new TaskResult();
        }
        TaskResult res = new TaskResult();
        if (
            params.hasParam(ACT_SUBMIT_ANSWER)
                || params.hasParam(ACT_SHOW_ANS)
                || params.hasParam(ACT_COMPLETE_TASK)
                || params.hasKeyValueParam(ACT_COMPLETE_TASK_WITH_MARK)
        ) {
            userAnswer = params.getParam(PAR_USER_ANS).trim();
            userAnswerIsCorrect = exactMatch ? Optional.of(expAnswer.equals(userAnswer)) : Optional.empty();
            if (histRec.isEmpty() && (userAnswerIsCorrect.isPresent() || params.hasParam(ACT_SHOW_ANS))) {
                BigDecimal mark = params.hasParam(ACT_SHOW_ANS)
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(userAnswerIsCorrect.get() ? 1 : 0);
                String notes = params.hasParam(ACT_SHOW_ANS)
                    ? utils.makeExpectedActualPair("", "<<<show_answer>>>")
                    : userAnswerIsCorrect.get() ? "" : utils.makeExpectedActualPair(expAnswer, userAnswer);
                histRec = Optional.of(
                    HistRec.builder()
                        .time(clock.instant())
                        .taskType(taskType.getCode())
                        .mark(mark)
                        .notes(notes)
                        .build()
                );
                res.setHistRec(histRec);
            }
            if (params.hasParam(ACT_SUBMIT_ANSWER)) {
                showAnswer = !exactMatch || userAnswerIsCorrect.orElse(false);
            }
            if (params.hasParam(ACT_SHOW_ANS)) {
                showAnswer = true;
            }
            if (params.hasParam(ACT_COMPLETE_TASK)) {
                res.setCompleted(true);
            }
            if (params.hasKeyValueParam(ACT_COMPLETE_TASK_WITH_MARK)) {
                res.setCompleted(true);
                if (histRec.isEmpty()) {
                    histRec = Optional.of(
                        HistRec.builder()
                            .time(clock.instant())
                            .taskType(taskType.getCode())
                            .mark(BigDecimal.valueOf(params.getKeyValueParamLong(ACT_COMPLETE_TASK_WITH_MARK)))
                            .notes(utils.makeExpectedActualPair("<<<assessed_by_user>>>", userAnswer))
                            .build()
                    );
                    res.setHistRec(histRec);
                }
            }
        }
        return res;
    }

    @Override
    public boolean isHistoryUpdated() {
        return histRec.isPresent();
    }

    @Override
    public HtmlElem render() {
        if (CollectionUtils.isNotEmpty(cardErrors)) {
            return rndErrors(cardErrors);
        }
        if (userAnswer == null) {
            userAnswer = "";
        }
        return frag(
            div(text(String.format("%s -> %s", taskType.getLangFrom(), taskType.getLangTo()))),
            br(),
            div(text(textToTranslate)),
            br(),
            div(rndUserAnswer()),
            br(),
            div(rndButtons()),
            br(),
            div(rndAnswerAndNote())
        );
    }

    private HtmlElem rndUserAnswer() {
        List<HtmlElem> content = new ArrayList<>();
        HtmlTag inpText = inpText(PAR_USER_ANS, userAnswer, ACT_SUBMIT_ANSWER).attr("size", "200");
        content.add(inpText);
        if (showAnswer) {
            inpText.attr("disabled", "");
            content.add(inpHidden(PAR_USER_ANS, userAnswer));
        }
        return frag(content);
    }

    private HtmlElem rndAnswerAndNote() {
        if (!showAnswer) {
            return null;
        }
        return frag(
            h4(text("Answer")),
            pre(text(expAnswer)),
            h4(text("Notes")),
            pre(text(card.getNotes()))
        );
    }

    private HtmlElem rndButtons() {
        HtmlTag submitAnswerBtn = inpSubmit(ACT_SUBMIT_ANSWER, "Submit answer");
        if (showAnswer) {
            submitAnswerBtn.attr("disabled", "");
        }
        HtmlTag showAnswerBtn = inpSubmit(ACT_SHOW_ANS, "Show answer");
        if (showAnswer) {
            showAnswerBtn.attr("disabled", "");
        }
        HtmlElem nextTaskBtn;
        if (showAnswer) {
            if (exactMatch) {
                nextTaskBtn = frag(
                    inpSubmit(ACT_COMPLETE_TASK, "Next task").attr("style", "background-color: green;"),
                    inpText("", "", ACT_COMPLETE_TASK).attr("size", "1")
                );
            } else {
                String mark0 = keyValueParam(ACT_COMPLETE_TASK_WITH_MARK, 0);
                String mark1 = keyValueParam(ACT_COMPLETE_TASK_WITH_MARK, 1);
                nextTaskBtn = frag(
                    inpSubmit(mark0, "Incorrect").attr("style", "background-color: red;"),
                    inpText("", "", mark0).attr("size", "1"),
                    inpSubmit(mark1, "Correct").attr("style", "background-color: green;"),
                    inpText("", "", mark1).attr("size", "1")
                );
            }
        } else {
            nextTaskBtn = null;
        }

        return frag(submitAnswerBtn, showAnswerBtn, nextTaskBtn);
    }

    private HtmlElem rndErrors(List<String> errors) {
        if (CollectionUtils.isEmpty(errors)) {
            return null;
        }
        return div("color:red;",
            h3(text("Errors in the card")),
            ul(errors.stream().map(msg -> pre(text(msg))).toList())
        );
    }
}
