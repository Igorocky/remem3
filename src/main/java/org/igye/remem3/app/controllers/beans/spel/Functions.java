package org.igye.remem3.app.controllers.beans.spel;

import lombok.SneakyThrows;
import org.igye.remem3.app.controllers.beans.dto.TaskFilter;
import org.springframework.expression.EvaluationContext;

import java.time.Instant;
import java.util.function.Supplier;

public class Functions {

    public static void registerFunctions(EvaluationContext evalContext) {
        registerFunction(evalContext, "supplier", Object.class);
        registerFunction(evalContext, "taskFilter", TaskFilter.class);
        registerFunction(evalContext, "instant", Instant.class);
    }

    public static TaskFilter taskFilter(TaskFilter taskFilter) {
        return taskFilter;
    }

    public static Instant instant(Instant instant) {
        return instant;
    }

    public static <T> Supplier<T> supplier(T obj) {
        return () -> obj;
    }

    @SneakyThrows
    private static void registerFunction(EvaluationContext evalCtx, String methodName, Class<?>... parameterTypes) {
        evalCtx.setVariable(methodName, Functions.class.getMethod(methodName, parameterTypes));
    }
}
