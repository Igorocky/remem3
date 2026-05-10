package org.igye.remem3.app.controllers.beans.spel;

import lombok.SneakyThrows;
import org.igye.remem3.app.controllers.beans.dto.TaskFilter;
import org.springframework.expression.EvaluationContext;

public class Functions {

    public static void registerFunctions(EvaluationContext evalContext) {
        registerFunction(evalContext, "taskFilter", TaskFilter.class);
    }

    public static TaskFilter taskFilter(TaskFilter taskFilter) {
        return taskFilter;
    }

    @SneakyThrows
    private static void registerFunction(EvaluationContext evalCtx, String methodName, Class<?>... parameterTypes) {
        evalCtx.setVariable(methodName, Functions.class.getMethod(methodName, parameterTypes));
    }
}
