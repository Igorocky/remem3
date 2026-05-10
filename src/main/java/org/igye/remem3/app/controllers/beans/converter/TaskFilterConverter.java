package org.igye.remem3.app.controllers.beans.converter;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.controllers.beans.dto.TaskFilter;
import org.igye.remem3.app.controllers.beans.spel.SpelEvaluator;
import org.springframework.core.convert.converter.Converter;

@RequiredArgsConstructor
public class TaskFilterConverter implements Converter<String, TaskFilter> {
    private final SpelEvaluator spelEvaluator;

    @Override
    public TaskFilter convert(String expr) {
        return task -> spelEvaluator.eval(task, expr, Boolean.class);
    }
}
