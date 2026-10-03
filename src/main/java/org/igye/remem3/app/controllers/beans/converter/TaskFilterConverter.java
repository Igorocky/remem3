package org.igye.remem3.app.controllers.beans.converter;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.Shell;
import org.igye.remem3.app.controllers.beans.dto.TaskFilter;
import org.igye.remem3.app.controllers.beans.dto.TaskView;
import org.springframework.core.convert.converter.Converter;

@RequiredArgsConstructor
public class TaskFilterConverter implements Converter<String, TaskFilter> {
    private final Shell sh;

    @Override
    public TaskFilter convert(String script) {
        return new TaskFilter() {
            @Override
            public boolean match(TaskView task) {
                return (boolean) sh.runScript(script, task);
            }

            @Override
            public String toString() {
                return script;
            }
        };
    }
}
