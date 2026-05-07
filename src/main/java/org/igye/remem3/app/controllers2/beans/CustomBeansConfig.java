package org.igye.remem3.app.controllers2.beans;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.ConversionService;

import java.util.Set;

@Configuration
@ImportResource("${app.beans-file}")
public class CustomBeansConfig {
    @Autowired
    private ApplicationContext ctx;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public GeneralFactoryBean generalFactoryBean() {
        return new GeneralFactoryBean();
    }

    @Bean
    public SpelEvaluator spelEvaluator(ObjectProvider<ConversionService> conversionService) {
        return new SpelEvaluatorImpl(conversionService);
    }

    @Bean
    public TaskFilterConverter taskFilterConverter(ObjectProvider<ConversionService> conversionService) {
        return new TaskFilterConverter(spelEvaluator(conversionService));
    }

    @Bean
    public ConversionServiceFactoryBean conversionService(ObjectProvider<ConversionService> conversionService) {
        ConversionServiceFactoryBean bean = new ConversionServiceFactoryBean();
        bean.setConverters(Set.of(
            taskFilterConverter(conversionService)
        ));
        return bean;
    }
}
