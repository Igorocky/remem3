package org.igye.remem3.app.controllers2.beans;

import lombok.SneakyThrows;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class CustomBeanExpressionResolver extends StandardBeanExpressionResolver {

    private static final String REVERSE = "reverse";

    @SneakyThrows
    @Override
    protected void customizeEvaluationContext(StandardEvaluationContext evalContext) {
        super.customizeEvaluationContext(evalContext);
        evalContext.registerFunction(REVERSE, Functions.class.getMethod(REVERSE, String.class));
    }

//    private void registerFunction(
//        StandardEvaluationContext evalContext
//    )
}
