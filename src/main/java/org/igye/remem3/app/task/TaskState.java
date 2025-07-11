package org.igye.remem3.app.task;

import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.web.RequestParams;

import java.util.List;

public interface TaskState {
    List<TaskResult> processUserInput(RequestParams params);

    HtmlElem render();
}
