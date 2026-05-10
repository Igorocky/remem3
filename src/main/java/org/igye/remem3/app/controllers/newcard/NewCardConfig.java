package org.igye.remem3.app.controllers.newcard;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(
    basePackages = "org.igye.remem3",
    useDefaultFilters = false,
    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        NewCardUtilsImpl.class,
        NewCardConstructor.class,
        NewCardUpdater.class,
        NewCardRenderer.class,
    })
)
public class NewCardConfig {
}
