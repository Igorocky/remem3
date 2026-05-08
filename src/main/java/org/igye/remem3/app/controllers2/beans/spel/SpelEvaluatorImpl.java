package org.igye.remem3.app.controllers2.beans.spel;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;

public class SpelEvaluatorImpl implements SpelEvaluator {
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ObjectProvider<ConversionService> conversionService;
    private volatile StandardEvaluationContext context;

    public SpelEvaluatorImpl(ObjectProvider<ConversionService> conversionService) {
        this.conversionService = conversionService;
    }

    private EvaluationContext getContext() {
        if (context == null) {
            synchronized (parser) {
                if (context == null) {
                    context = new StandardEvaluationContext();
                    context.setTypeConverter(new StandardTypeConverter(conversionService.getObject()));
                    context.setOperatorOverloader(new OperatorOverloaderImpl(conversionService.getObject()));
                    Functions.registerFunctions(context);
                }
            }
        }
        return context;
    }

    @Override
    public <T> T eval(Object rootObj, String expr, Class<T> type) {
        return parser.parseExpression(expr).getValue(getContext(), rootObj, type);
    }
}
