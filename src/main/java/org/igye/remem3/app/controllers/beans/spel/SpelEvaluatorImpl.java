package org.igye.remem3.app.controllers.beans.spel;

import org.springframework.core.convert.ConversionService;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;

public class SpelEvaluatorImpl implements SpelEvaluator {
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private StandardEvaluationContext context;

    public void setConversionService(ConversionService conversionService) {
        context = new StandardEvaluationContext();
        context.setTypeConverter(new StandardTypeConverter(conversionService));
        context.setOperatorOverloader(new OperatorOverloaderImpl(conversionService));
        Functions.registerFunctions(context);
    }

    @Override
    public <T> T eval(Object rootObj, String expr, Class<T> type) {
        return parser.parseExpression(expr).getValue(context, rootObj, type);
    }
}
