package org.igye.remem3.app.task.impl;

import org.igye.remem3.app.task.TaskResult;
import org.igye.remem3.app.task.TaskState;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.web.RequestParams;

public class TaskStateTranslate extends HtmlBuilder implements TaskState {

    @Override
    public TaskResult processUserInput(RequestParams params) {
        throw new Exn("not implemented");
    }

    @Override
    public boolean isHistoryUpdated() {
        throw new Exn("not implemented");
    }

    @Override
    public HtmlElem render() {
        throw new Exn("not implemented");
    }
}
