package org.igye.remem3.app.task;

import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.web.RequestParams;

public interface TaskState {
    TaskResult processUserInput(RequestParams params);

    boolean isHistoryUpdated();

    HtmlElem render();
}
