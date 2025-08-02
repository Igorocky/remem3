package org.igye.remem3.app.task.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.igye.remem3.app.CardUtils;
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

import static java.lang.String.format;

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

    private boolean hasMissingAnswer;
    private String userAnswer;
    private Optional<Boolean> userAnswerIsCorrect = Optional.empty();
    private Optional<HistRec> histRec = Optional.empty();
    private boolean showAnswer;

    public TaskStateTranslate(Clock clock, Utils utils, CardUtils cardUtils, Card.Translate card,
                              TaskType.Translate taskType) {
        this.clock = clock;
        this.utils = utils;
        this.card = card;
        this.taskType = taskType;
        cardErrors = new ArrayList<>(cardUtils.validateCard(card));
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
            cardErrors.add(format(
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
        hasMissingAnswer = false;
        if (
            params.hasParam(ACT_SUBMIT_ANSWER)
                || params.hasParam(ACT_SHOW_ANS)
                || params.hasParam(ACT_COMPLETE_TASK)
                || params.hasKeyValueParam(ACT_COMPLETE_TASK_WITH_MARK)
        ) {
            userAnswer = params.getParam(PAR_USER_ANS).trim();
            hasMissingAnswer = exactMatch && StringUtils.isBlank(userAnswer);
            userAnswerIsCorrect = exactMatch
                ? (hasMissingAnswer ? Optional.empty() : Optional.of(expAnswer.equals(userAnswer)))
                : Optional.empty();
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
                showAnswer = showAnswer || !exactMatch || userAnswerIsCorrect.orElse(false);
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
            div(text(format("%s -> %s", taskType.getLangFrom(), taskType.getLangTo()))),
            br(),
            div(text(textToTranslate)),
            br(),
            div(rndUserAnswer()),
            br(),
            hasMissingAnswer ? div("color:red;margin-bottom:20px;", text("Please provide an answer.")) : null,
            br(),
            div(rndButtons()),
            br(),
            div(rndAnswerAndNote())
        );
    }

    private HtmlElem rndUserAnswer() {
        List<HtmlElem> content = new ArrayList<>();
        content.add(text(exactMatch ? "= " : "~ "));
        HtmlTag inpText = inpText(PAR_USER_ANS, userAnswer, ACT_SUBMIT_ANSWER, true)
            .attr("size", "150").attr("spellcheck", "false").attr("class", "border-on-focus");
        content.add(inpText);
        if (!exactMatch && showAnswer || userAnswerIsCorrect.orElse(false)) {
            inpText.disabled();
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
        ArrayList<HtmlElem> content = new ArrayList<>();
        if (!showAnswer) {
            content.add(
                inpSubmit(ACT_SHOW_ANS, "Show answer")
                    .attr("style", format("background-color: %s;", ORANGE)).attr("class", "border-on-focus")
            );
        } else {
            if (exactMatch && userAnswerIsCorrect.orElse(false) || !exactMatch && histRec.isPresent()) {
                content.add(frag(
                    inpSubmit(ACT_COMPLETE_TASK, "Next task")
                        .attr("style", format("background-color: %s;", GREEN)).attr("class", "border-on-focus")
                        .attr("autofocus", "")
                ));
            }
            if (!exactMatch && histRec.isEmpty()) {
                String parMark0 = keyValueParam(ACT_COMPLETE_TASK_WITH_MARK, 0);
                String parMark1 = keyValueParam(ACT_COMPLETE_TASK_WITH_MARK, 1);
                content.add(table(List.of(
                    List.of(
                        inpSubmit(parMark0, "Incorrect")
                            .attr("style", format("background-color: %s;", RED)).attr("class", "border-on-focus")
                            .attr("autofocus", ""),
                        inpSubmit(parMark1, "Correct")
                            .attr("style", format("background-color: %s;", GREEN)).attr("class", "border-on-focus")
                    )
                )));
            }
        }
        if (!exactMatch && !showAnswer || exactMatch && !userAnswerIsCorrect.orElse(false)) {
            content.add(inpSubmit(ACT_SUBMIT_ANSWER, "Submit answer").attr("class", "border-on-focus"));
        }
        return frag(content);
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
