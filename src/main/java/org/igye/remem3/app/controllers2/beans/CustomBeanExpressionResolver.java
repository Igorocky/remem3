package org.igye.remem3.app.controllers2.beans;

import lombok.SneakyThrows;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class CustomBeanExpressionResolver extends StandardBeanExpressionResolver {

    @Override
    protected void customizeEvaluationContext(StandardEvaluationContext evalContext) {
        super.customizeEvaluationContext(evalContext);
        registerFunction(evalContext, "taskTypeIn", String[].class);
    }

    @SneakyThrows
    private void registerFunction(StandardEvaluationContext evalCtx, String methodName, Class<?>... parameterTypes) {
        evalCtx.registerFunction(methodName, Functions.class.getMethod(methodName, parameterTypes));
    }
}
