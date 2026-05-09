package org.igye.remem3.app.controllers2.validatecards;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(
    basePackages = "org.igye.remem3",
    useDefaultFilters = false,
    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        ValidateCardsConstructor.class,
        ValidateCardsUpdater.class,
        ValidateCardsRenderer.class,
    })
)
public class ValidateCardsConfig {
}
