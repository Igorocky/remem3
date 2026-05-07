package org.igye.remem3.app.controllers2.beans;

import lombok.SneakyThrows;
import org.igye.remem3.app.TaskTypeMatcher;
import org.igye.remem3.app.controllers2.beans.dto.TaskFilter;
import org.igye.remem3.app.impl.TaskTypeMatcherImpl;
import org.springframework.expression.EvaluationContext;

import java.util.Arrays;

public class Functions {

    public static void registerFunctions(EvaluationContext evalContext) {
        registerFunction(evalContext, "taskType", String[].class);
    }

    public static TaskFilter taskType(String... types) {
        TaskTypeMatcher taskTypeMatcher = Arrays.stream(types)
            .map(TaskTypeMatcherImpl::new)
            .map(TaskTypeMatcher.class::cast)
            .reduce(TaskTypeMatcher::or)
            .get();
        return task -> taskTypeMatcher.matches(task.getType());
    }

    @SneakyThrows
    private static void registerFunction(EvaluationContext evalCtx, String methodName, Class<?>... parameterTypes) {
        evalCtx.setVariable(methodName, Functions.class.getMethod(methodName, parameterTypes));
    }
}
