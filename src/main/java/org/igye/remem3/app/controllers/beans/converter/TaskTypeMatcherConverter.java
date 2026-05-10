package org.igye.remem3.app.controllers.beans.converter;

import org.igye.remem3.app.TaskTypeMatcher;
import org.igye.remem3.app.impl.TaskTypeMatcherImpl;
import org.springframework.core.convert.converter.Converter;

import java.util.concurrent.ConcurrentHashMap;

public class TaskTypeMatcherConverter implements Converter<String, TaskTypeMatcher> {
    private final ConcurrentHashMap<String, TaskTypeMatcher> taskTypeMatchers = new ConcurrentHashMap<>();

    @Override
    public TaskTypeMatcher convert(String patt) {
        TaskTypeMatcher res = taskTypeMatchers.get(patt);
        if (res == null) {
            res = new TaskTypeMatcherImpl(patt);
            taskTypeMatchers.put(patt, res);
        }
        return res;
    }
}
