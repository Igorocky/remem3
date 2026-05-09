package org.igye.remem3.app.controllers2.beans;

import org.igye.remem3.app.controllers2.beans.converter.TaskFilterConverter;
import org.igye.remem3.app.controllers2.beans.converter.TaskTypeMatcherConverter;
import org.igye.remem3.app.controllers2.beans.spel.SpelEvaluatorImpl;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionService;

import java.util.Set;

public class CustomConversionServiceFactoryBean extends ConversionServiceFactoryBean {
    private final SpelEvaluatorImpl spelEvaluator;

    public CustomConversionServiceFactoryBean() {
        spelEvaluator = new SpelEvaluatorImpl();
        setConverters(Set.of(
            new TaskFilterConverter(spelEvaluator),
            new TaskTypeMatcherConverter()
        ));
    }

    @Override
    public ConversionService getObject() {
        ConversionService conversionService = super.getObject();
        spelEvaluator.setConversionService(conversionService);
        return conversionService;
    }
}
