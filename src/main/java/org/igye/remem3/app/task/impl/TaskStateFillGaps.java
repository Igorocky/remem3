package org.igye.remem3.app.task.impl;

import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.app.dto.fillgaps.TextPart;
import org.igye.remem3.app.task.TaskResult;
import org.igye.remem3.app.task.TaskState;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.web.RequestParams;

import java.util.List;
import java.util.Optional;

public class TaskStateFillGaps extends HtmlBuilder implements TaskState {
    private final Task.FillGaps task;
    private final Card.FillGaps card;
    private Optional<HistRec> histRecOpt = Optional.empty();
    private List<String> userAnswers = List.of();

    public TaskStateFillGaps(Task.FillGaps task) {
        this.task = task;
        this.card = task.getCard();
    }

    @Override
    public List<TaskResult> processUserInput(RequestParams params) {
        return List.of(new TaskResult.Completed());
    }

    @Override
    public boolean isHistoryUpdated() {
        return histRecOpt.isPresent();
    }

    @Override
    public HtmlElem render() {
        return frag(
            h6(text(String.format("Fill gaps in %s language", card.getLang()))),
            rndTextWithGaps(card.getText())
        );
    }

    private HtmlElem rndTextWithGaps(List<TextPart> text) {
        return null;
    }
}
