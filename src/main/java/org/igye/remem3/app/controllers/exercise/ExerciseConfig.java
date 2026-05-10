package org.igye.remem3.app.controllers.exercise;

import org.igye.remem3.app.state.StateLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Configuration
@ComponentScan(
    basePackages = "org.igye.remem3",
    useDefaultFilters = false,
    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        ExerciseConstructor.class,
        ExerciseUpdater.class,
        ExerciseRenderer.class,
    })
)
public class ExerciseConfig {
    @Autowired
    private ApplicationContext ctx;

    @EventListener(ContextRefreshedEvent.class)
    public void handleContextRefresh() {
        ctx.getBean(ExerciseConstructor.class).setStateLookup(ctx.getBean(StateLookup.class));
    }
}
