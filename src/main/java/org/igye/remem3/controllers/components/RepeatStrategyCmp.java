package org.igye.remem3.controllers.components;

import org.igye.remem3.app.RepeatStrategy;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.html.HtmlElem;

import java.util.List;

public interface RepeatStrategyCmp {
    HtmlElem render();

    RepeatStrategy makeRepeatStrategy(List<Task> tasks);
}
