package org.igye.remem3.app.controllers2.beans;

import org.igye.remem3.app.controllers2.beans.converter.DurationConverter;
import org.igye.remem3.app.controllers2.beans.converter.InstantConverter;
import org.igye.remem3.app.controllers2.beans.converter.TaskFilterConverter;
import org.igye.remem3.app.controllers2.beans.converter.TaskTypeMatcherConverter;
import org.igye.remem3.app.controllers2.beans.spel.SpelEvaluator;
import org.igye.remem3.app.controllers2.beans.spel.SpelEvaluatorImpl;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.utils.impl.UtilsImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

@Configuration("__customBeansConfig")
@ImportResource("${app.beans-file}")
public class CustomBeansConfig {

    @Bean("__propertySourcesPlaceholderConfigurer")
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean("__objectMapper")
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean("__utils")
    public Utils utils() {
        return new UtilsImpl(objectMapper());
    }

    @Bean("__spelEvaluator")
    public SpelEvaluator spelEvaluator() {
        return new SpelEvaluatorImpl();
    }

    @Bean("__taskFilterConverter")
    public TaskFilterConverter taskFilterConverter() {
        return new TaskFilterConverter(spelEvaluator());
    }

    @Bean("__taskTypeMatcherConverter")
    public TaskTypeMatcherConverter taskTypeMatcherConverter() {
        return new TaskTypeMatcherConverter();
    }

    @Bean("__durationConverter")
    public DurationConverter durationConverter() {
        return new DurationConverter(utils());
    }

    @Bean("__instantConverter")
    public InstantConverter instantConverter() {
        return new InstantConverter();
    }

    @Bean
    public ConversionServiceFactoryBean conversionService() {
        CustomConversionServiceFactoryBean factoryBean = new CustomConversionServiceFactoryBean(
            ((SpelEvaluatorImpl) spelEvaluator())::setConversionService
        );
        factoryBean.setConverters(Set.of(
            taskFilterConverter(),
            taskTypeMatcherConverter(),
            durationConverter(),
            instantConverter()
        ));
        return factoryBean;
    }

}
