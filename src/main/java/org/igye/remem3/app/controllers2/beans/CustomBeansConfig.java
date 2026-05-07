package org.igye.remem3.app.controllers2.beans;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
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
    public GeneralFactoryBean generalFactoryBean() {
        return new GeneralFactoryBean();
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
    public ConversionServiceFactoryBean conversionServiceFactoryBean() {
        ConversionServiceFactoryBean bean = new ConversionServiceFactoryBean();
        bean.setConverters(Set.of(
            taskFilterConverter()
        ));
        return bean;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @EventListener(ContextRefreshedEvent.class)
    public void handleContextRefresh() {
        ctx.getBean(SpelEvaluatorImpl.class).setConversionService(ctx.getBean(ConversionService.class));
    }
}
