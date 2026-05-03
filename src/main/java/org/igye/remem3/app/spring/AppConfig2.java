package org.igye.remem3.app.spring;

import org.igye.remem3.app.controllers2.counter.CounterConfig;
import org.igye.remem3.app.controllers2.index.IndexConfig;
import org.igye.remem3.app.controllers2.newcard.NewCardConfig;
import org.igye.remem3.app.state.StateConstructor;
import org.igye.remem3.app.state.StateRenderer;
import org.igye.remem3.app.state.StateRepository;
import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.app.state.impl.StateRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Configuration
@Import({IndexConfig.class, CounterConfig.class, NewCardConfig.class})
public class AppConfig2 {

    @Bean
    public StateRepository stateRepository(
        Clock clock,
        List<StateConstructor> stateConstructors,
        List<StateUpdater<?, ?>> stateUpdaters,
        List<StateRenderer<?>> stateRenderers
    ) {
        return new StateRepositoryImpl(clock, Duration.ofHours(1), stateConstructors, stateUpdaters, stateRenderers);
    }
}
