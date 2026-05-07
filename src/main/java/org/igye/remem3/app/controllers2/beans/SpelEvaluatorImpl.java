package org.igye.remem3.app.controllers2.beans;

import lombok.Setter;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

public class SpelEvaluatorImpl implements SpelEvaluator {
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private EvaluationContext context;

    @Setter
    private ConversionService conversionService;

    private EvaluationContext getContext() {
        if (context == null) {
            context = SimpleEvaluationContext.forReadOnlyDataBinding()
                .withConversionService(conversionService)
                .build();
            Functions.registerFunctions(context);
        }
        return context;
    }

    @Override
    public <T> T eval(Object rootObj, String expr, Class<T> type) {
        return parser.parseExpression(expr).getValue(getContext(), rootObj, type);
    }
}
