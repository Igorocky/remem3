package org.igye.remem3.app.controllers2.beans;

import org.igye.remem3.app.controllers2.beans.converter.TaskFilterConverter;
import org.igye.remem3.app.controllers2.beans.converter.TaskTypeMatcherConverter;
import org.igye.remem3.app.controllers2.beans.spel.SpelEvaluator;
import org.igye.remem3.app.controllers2.beans.spel.SpelEvaluatorImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.Set;

@Configuration
@ImportResource("${app.beans-file}")
public class CustomBeansConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public SpelEvaluator spelEvaluator() {
        return new SpelEvaluatorImpl();
    }

    @Bean
    public TaskFilterConverter taskFilterConverter() {
        return new TaskFilterConverter(spelEvaluator());
    }

    @Bean
    public TaskTypeMatcherConverter taskTypeMatcherConverter() {
        return new TaskTypeMatcherConverter();
    }

    @Bean
    public ConversionServiceFactoryBean conversionService() {
        CustomConversionServiceFactoryBean factoryBean = new CustomConversionServiceFactoryBean(
            ((SpelEvaluatorImpl) spelEvaluator())::setConversionService
        );
        factoryBean.setConverters(Set.of(
            taskFilterConverter(),
            taskTypeMatcherConverter()
        ));
        return factoryBean;
    }

}
