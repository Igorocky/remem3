package org.igye.remem3.app.task.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.igye.remem3.app.Cards;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.app.dto.fillgaps.TextPart;
import org.igye.remem3.app.task.TaskResult;
import org.igye.remem3.app.task.TaskState;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.web.RequestParams;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskStateFillGaps extends HtmlBuilder implements TaskState {
    private static final String PAR_USER_ANS = "PAR_USER_ANS";
    private static final String ACT_SUBMIT_ANSWERS = "ACT_SUBMIT_ANSWERS";
    private static final String ACT_SHOW_HINT = "ACT_SHOW_HINT";
    private static final String ACT_SHOW_ANS = "ACT_SHOW_ANS";
    private static final String ACT_COMPLETE_TASK = "ACT_COMPLETE_TASK";

    private final Task.FillGaps task;
    private final Card.FillGaps card;
    private List<String> cardErrors;
    private List<TextPart.Gap> gaps;
    private List<String> userAnswers;
    private boolean allAnsAreCorrect;
    private Optional<HistRec> histRec = Optional.empty();
    private boolean showHints;
    private boolean showAnswers;

    public TaskStateFillGaps(Cards cards, Task.FillGaps task) {
        this.task = task;
        card = task.getCard();
        cardErrors = cards.validateCard(card);
        if (CollectionUtils.isEmpty(cardErrors)) {
            gaps = card.getText().stream()
                .filter(p -> p instanceof TextPart.Gap)
                .map(p -> (TextPart.Gap) p)
                .toList();
        }
    }

    @Override
    public TaskResult processUserInput(RequestParams params) {
        if (!CollectionUtils.isEmpty(cardErrors)) {
            return new TaskResult();
        }
        readUserAnswers(Optional.of(params));
        TaskResult res = new TaskResult();
        if (histRec.isEmpty()) {
            histRec = Optional.of(makeHistRec());
            res.setHistRec(histRec);
        }
        if (params.hasParam(ACT_COMPLETE_TASK)) {
            res.setCompleted(Optional.of(true));
        }
        if (params.hasParam(ACT_SHOW_HINT)) {
            showHints = true;
        }
        if (params.hasParam(ACT_SHOW_ANS)) {
            showAnswers = true;
        }
        return res;
    }

    @Override
    public boolean isHistoryUpdated() {
        return histRec.isPresent();
    }

    @Override
    public HtmlElem render() {
        if (!CollectionUtils.isEmpty(cardErrors)) {
            return rndErrors(cardErrors);
        }
        if (CollectionUtils.isEmpty(userAnswers)) {
            readUserAnswers(Optional.empty());
        }
        HtmlElem descr = StringUtils.isNotBlank(card.getDescr())
            ? text(card.getDescr())
            : text(String.format("Fill gaps in %s language", card.getLang()));
        return frag(
            descr,
            h("br"),
            rndTextWithGaps(),
            h("br"),
            rndButtons(),
            h("br"),
            rndAnswers(),
            h("br"),
            rndNote()
        );
    }

    private HtmlElem rndNote() {
        if (allAnsAreCorrect) {
            return text(card.getNotes());
        }
        return null;
    }

    private HtmlElem rndAnswers() {
        ArrayList<List<? extends HtmlElem>> rows = new ArrayList<>();
        for (int i = 0; i < gaps.size(); i++) {
            TextPart.Gap gap = gaps.get(i);
            String userAns = userAnswers.get(i);
            ArrayList<HtmlElem> row = new ArrayList<>();
            rows.add(row);
            row.add(text((i + 1) + ")"));
            boolean ansIsCorrect = gap.getAnswer().equals(userAns);
            if (ansIsCorrect || showAnswers) {
                row.add(h("b", text(gap.getAnswer())));
            } else {
                row.add(text(""));
            }
            if (ansIsCorrect || showHints) {
                row.add(text(gap.getHint()));
            } else {
                row.add(text(""));
            }
            if (ansIsCorrect) {
                row.add(text(gap.getNotes()));
            } else {
                row.add(text(""));
            }
        }
        return table(rows);
    }

    private HtmlElem rndButtons() {
        HtmlTag submitAnswersBtn = inpSubmit(ACT_SUBMIT_ANSWERS, "Submit answers");
        if (allAnsAreCorrect) {
            submitAnswersBtn.addAttr("disabled", "");
        }
        HtmlTag showHintBtn = inpSubmit(ACT_SHOW_HINT, "Hint");
        if (showHints || allAnsAreCorrect) {
            showHintBtn.addAttr("disabled", "");
        }
        HtmlTag showAnswerBtn = inpSubmit(ACT_SHOW_ANS, "Show answer");
        if (showAnswers || allAnsAreCorrect) {
            showAnswerBtn.addAttr("disabled", "");
        }
        HtmlTag nextTaskBtn = inpSubmit(ACT_COMPLETE_TASK, "Next task");
        if (!allAnsAreCorrect) {
            nextTaskBtn.addAttr("disabled", "");
        }

        return frag(
            submitAnswersBtn,
            showHintBtn,
            showAnswerBtn,
            nextTaskBtn,
            allAnsAreCorrect
                ? inpText("", "", ACT_COMPLETE_TASK).addAttr("size", "1")
                : null
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
                    HtmlTag gapElem = inpText(gapParamName, userAns, ACT_SUBMIT_ANSWERS)
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
    }
}
