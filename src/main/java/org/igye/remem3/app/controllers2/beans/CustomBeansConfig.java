package org.igye.remem3.app.controllers2.beans;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ConversionServiceFactoryBean;

@Configuration
public class CustomBeansConfig {
    @Bean
    public GeneralFactoryBean generalFactoryBean() {
        return new GeneralFactoryBean();
    }

    @Bean
    public ConversionServiceFactoryBean conversionService() {
        return new ConversionServiceFactoryBean();
    }
}
