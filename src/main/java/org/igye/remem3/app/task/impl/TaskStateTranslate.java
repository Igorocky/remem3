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
import org.igye.remem3.web.RequestParams;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskStateTranslate extends HtmlBuilder implements TaskState {
    private static final String PAR_USER_ANS = "PAR_USER_ANS";
    private static final String PAR_USER_MARK = "PAR_USER_MARK";
    private static final String ACT_SUBMIT_ANSWER = "ACT_SUBMIT_ANSWER";
    private static final String ACT_SHOW_ANS = "ACT_SHOW_ANS";
    private static final String ACT_COMPLETE_TASK = "ACT_COMPLETE_TASK";

    private final Card.Translate card;
    private final TaskType.Translate taskType;
    private final List<String> cardErrors;
    private final String question;
    private final String expAnswer;
    private final boolean expAnsIsReadOnly;

    private String userAnswer;
    private boolean userAnswerIsCorrect;
    private Optional<HistRec> histRec = Optional.empty();
    private boolean showAnswer;

    public TaskStateTranslate(Cards cards, Card.Translate card, TaskType.Translate taskType) {
        this.card = card;
        this.taskType = taskType;
        cardErrors = new ArrayList<>(cards.validateCard(card));
        if (CollectionUtils.isNotEmpty(cardErrors)) {
            question = null;
            expAnswer = null;
            expAnsIsReadOnly = false;
            return;
        }
        if (card.getLang1().equals(taskType.getLangFrom()) && card.getLang2().equals(taskType.getLangTo())) {
            question = card.getText1();
            expAnswer = card.getText2();
            expAnsIsReadOnly = card.isReadOnly2();
        } else if (card.getLang2().equals(taskType.getLangFrom()) && card.getLang1().equals(taskType.getLangTo())) {
            question = card.getText2();
            expAnswer = card.getText1();
            expAnsIsReadOnly = card.isReadOnly1();
        } else {
            cardErrors.add(String.format(
                "Languages from the task %s->%s don't match languages in the card %s<->%s.",
                taskType.getLangFrom(), taskType.getLangTo(), card.getLang1(), card.getLang2()
            ));
            question = null;
            expAnswer = null;
            expAnsIsReadOnly = false;
        }
    }

    @Override
    public TaskResult processUserInput(RequestParams params) {
        if (CollectionUtils.isNotEmpty(cardErrors)) {
            return new TaskResult();
        }
        TaskResult res = new TaskResult();
        if (expAnsIsReadOnly && params.hasParam(ACT_SHOW_ANS)) {
            showAnswer = true;
        } else if (expAnsIsReadOnly && params.hasKeyValueParam(ACT_COMPLETE_TASK)) {
            res.setHistRec(Optional.of(
                HistRec.builder()
                    .time(Instant.now())
                    .taskType(taskType.getCode())
                    .mark(BigDecimal.valueOf(params.getKeyValueParamLong(ACT_COMPLETE_TASK)))
                    .notes("")
                    .build()
            ));
            res.setCompleted(true);
        }
//        readUserAnswers(Optional.of(params));
//        if (histRec.isEmpty()) {
//            histRec = Optional.of(makeHistRec());
//            res.setHistRec(histRec);
//        }
//        if (params.hasParam(ACT_COMPLETE_TASK)) {
//            res.setCompleted(Optional.of(true));
//        }
//        if (params.hasParam(ACT_SHOW_HINT)) {
//            showHints = true;
//        }
//        if (params.hasParam(ACT_SHOW_ANS)) {
//            showAnswer = true;
//        }
        return res;
    }

    @Override
    public boolean isHistoryUpdated() {
        return histRec.isPresent();
    }

    @Override
    public HtmlElem render() {
        return null;
//        if (CollectionUtils.isNotEmpty(cardErrors)) {
//            return rndErrors(cardErrors);
//        }
//        if (CollectionUtils.isEmpty(userAnswers)) {
//            readUserAnswers(Optional.empty());
//        }
//        HtmlElem descr = StringUtils.isNotBlank(card.getDescr())
//            ? text(card.getDescr())
//            : text(String.format("Fill gaps in %s language", card.getLang()));
//        return frag(
//            div(descr),
//            br(),
//            div(rndTextWithGaps()),
//            br(),
//            div(rndButtons()),
//            br(),
//            div(rndAnswers()),
//            br(),
//            div(rndNote())
//        );
    }

    /*private HtmlElem rndNote() {
        if (allAnsAreCorrect) {
            return text(card.getNotes());
        }
        return null;
    }

    private HtmlElem rndAnswers() {
        ArrayList<HtmlElem> items = new ArrayList<>();
        boolean nothingToShow = true;
        for (int i = 0; i < gaps.size(); i++) {
            TextPart.Gap gap = gaps.get(i);
            String userAns = userAnswers.get(i);
            ArrayList<HtmlElem> listItem = new ArrayList<>();
            boolean ansIsCorrect = gap.getAnswer().equals(userAns);
            if (ansIsCorrect || showAnswer) {
                listItem.add(h("b", text(gap.getAnswer())));
                nothingToShow = false;
            }
            if (StringUtils.isNotBlank(gap.getHint()) && (ansIsCorrect || showHints)) {
                if (!listItem.isEmpty()) {
                    listItem.add(br());
                }
                listItem.add(text(gap.getHint()));
                nothingToShow = false;
            }
            if (StringUtils.isNotBlank(gap.getNotes()) && ansIsCorrect) {
                listItem.add(br());
                listItem.add(text(gap.getNotes()));
                nothingToShow = false;
            }
            items.add(frag(listItem));
        }
        if (nothingToShow) {
            return null;
        }
        return ol(items);
    }

    private HtmlElem rndButtons() {
        HtmlTag submitAnswersBtn = inpSubmit(ACT_SUBMIT_ANSWER, "Submit answers");
        if (allAnsAreCorrect) {
            submitAnswersBtn.addAttr("disabled", "");
        }
        HtmlTag showHintBtn = inpSubmit(ACT_SHOW_HINT, "Hint");
        if (showHints || allAnsAreCorrect) {
            showHintBtn.addAttr("disabled", "");
        }
        HtmlTag showAnswerBtn = inpSubmit(ACT_SHOW_ANS, "Show answer");
        if (showAnswer || allAnsAreCorrect) {
            showAnswerBtn.addAttr("disabled", "");
        }

        return frag(
            submitAnswersBtn,
            showHintBtn,
            showAnswerBtn,
            !allAnsAreCorrect ? null : frag(
                inpSubmit(ACT_COMPLETE_TASK, "Next task").addAttr("style", "background-color: green;"),
                inpText("", "", ACT_COMPLETE_TASK).addAttr("size", "1")
            )
        );
    }

    private HistRec makeHistRec() {
        StringBuilder note = new StringBuilder();
        for (int i = 0; i < gaps.size(); i++) {
            String userAns = userAnswers.get(i);
            String expAns = gaps.get(i).getAnswer();
            if (!expAns.equals(userAns)) {
                note.append(" ###EXP ").append(expAns).append(" ###ACT ").append(userAns);
            }
        }
        return HistRec.builder()
            .time(Instant.now())
            .taskType(task.getTaskType().getCode())
            .mark(allAnsAreCorrect ? 1.0 : 0.0)
            .notes(note.toString().trim())
            .build();
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

    private void readUserAnswers(Optional<RequestParams> paramsOpt) {
        if (paramsOpt.isEmpty()) {
            userAnswers = gaps.stream().map(_ -> "").toList();
            allAnsAreCorrect = false;
            return;
        }
        RequestParams params = paramsOpt.get();
        userAnswers = new ArrayList<>();
        allAnsAreCorrect = true;
        for (int i = 0; i < gaps.size(); i++) {
            String paramName = keyValueParam(PAR_USER_ANS, i);
            if (!params.hasParam(paramName)) {
                throw new Exn(String.format("!params.hasParam(keyValueParam(%s, %s))", PAR_USER_ANS, i));
            }
            String userAns = params.getParam(paramName).trim();
            userAnswers.add(userAns);
            allAnsAreCorrect = allAnsAreCorrect && gaps.get(i).getAnswer().equals(userAns);
        }
    }

    private HtmlElem rndTextWithGaps() {
        ArrayList<HtmlElem> content = new ArrayList<>();
        int gapIdx = 0;
        for (TextPart textPart : card.getText()) {
            content.add(text(" "));
            switch (textPart) {
                case TextPart.Text text -> content.add(text(text.getText()));
                case TextPart.Gap gap -> {
                    String userAns = userAnswers.get(gapIdx);
                    String gapParamName = keyValueParam(PAR_USER_ANS, gapIdx);
                    HtmlTag gapElem = inpText(gapParamName, userAns, ACT_SUBMIT_ANSWER)
                        .addAttr("size", "20");
                    content.add(gapElem);
                    if (gap.getAnswer().equals(userAns)) {
                        gapElem.addAttr("disabled", "");
                        content.add(inpHidden(gapParamName, userAns));
                    }
                    gapIdx++;
                }
            }
        }
        return frag(content);
    }*/
}
