package org.igye.remem3.app.spring;

import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers.beans.BeansConfig;
import org.igye.remem3.app.controllers.beans.CustomBeansConfig;
import org.igye.remem3.app.controllers.convertfillgapstotrnaslate.ConvertFillGapsToTranslateConfig;
import org.igye.remem3.app.controllers.exercise.ExerciseConfig;
import org.igye.remem3.app.controllers.index.IndexConfig;
import org.igye.remem3.app.controllers.movecardstodir.MoveCardsToDirConfig;
import org.igye.remem3.app.controllers.newcard.NewCardConfig;
import org.igye.remem3.app.controllers.validatecards.ValidateCardsConfig;
import org.igye.remem3.app.impl.CacheImpl;
import org.igye.remem3.app.impl.CardUtilsImpl;
import org.igye.remem3.app.impl.SettingsImpl;
import org.igye.remem3.app.state.StateConstructor;
import org.igye.remem3.app.state.StateRenderer;
import org.igye.remem3.app.state.StateRepository;
import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.app.state.impl.StateRepositoryImpl;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.utils.impl.UtilsImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Configuration
@EnableConfigurationProperties(AppProps.class)
@ComponentScan(
    basePackages = "org.igye.remem3",
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CustomBeansConfig.class),
    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        UtilsImpl.class,
        CardUtilsImpl.class,
    })
)
@Import({
    IndexConfig.class, NewCardConfig.class, BeansConfig.class, ExerciseConfig.class, ValidateCardsConfig.class,
    ConvertFillGapsToTranslateConfig.class, MoveCardsToDirConfig.class
})
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public Settings settings(AppProps appProps, Utils utils) {
        return SettingsImpl.load(appProps, utils);
    }

    @Bean
    public Cache cache(Settings settings, Utils utils) {
        return new CacheImpl(utils, new File(settings.getCacheFile()));
    }

    @Bean
    public StateRepository stateRepository(
        Clock clock,
        List<StateConstructor<?>> stateConstructors,
        List<StateUpdater<?>> stateUpdaters,
        List<StateRenderer<?>> stateRenderers
    ) {
        return new StateRepositoryImpl(clock, Duration.ofHours(1), stateConstructors, stateUpdaters, stateRenderers);
    }
}
