package org.igye.remem3.app.controllers.cardimporter;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(
    basePackages = "org.igye.remem3",
    useDefaultFilters = false,
    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        CardImporterConstructor.class,
        CardImporterUpdater.class,
        CardImporterRenderer.class,
    })
)
public class CardImporterConfig {
}
