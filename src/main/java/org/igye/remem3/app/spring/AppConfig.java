package org.igye.remem3.app.spring;

import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers.IndexController;
import org.igye.remem3.app.controllers.convertfillgapstotrnaslate.ConvertFillGapsToTranslateController;
import org.igye.remem3.app.controllers.exercise.ExerciseController;
import org.igye.remem3.app.controllers.movecardstodir.MoveCardsToDirController;
import org.igye.remem3.app.controllers.newcard.NewCardController;
import org.igye.remem3.app.controllers.validatecards.ValidateCardsController;
import org.igye.remem3.app.controllers2.beans.BeansConfig;
import org.igye.remem3.app.controllers2.beans.CustomBeansConfig;
import org.igye.remem3.app.controllers2.exercise.ExerciseConfig;
import org.igye.remem3.app.controllers2.index.IndexConfig;
import org.igye.remem3.app.controllers2.newcard.NewCardConfig;
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
import org.igye.remem3.web.DispatcherController;
import org.igye.remem3.web.StatefulWebController;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties(AppProps.class)
@ComponentScan(
    basePackages = "org.igye.remem3",
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CustomBeansConfig.class),
    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        UtilsImpl.class,
        CardUtilsImpl.class,
        NewCardController.class,
        ExerciseController.class,
        ValidateCardsController.class,
        ConvertFillGapsToTranslateController.class,
        MoveCardsToDirController.class,
    })
)
@Import({IndexConfig.class, NewCardConfig.class, BeansConfig.class, ExerciseConfig.class})
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

    @Bean
    public IndexController indexController(
        NewCardController newCardController,
        ExerciseController exerciseController,
        ConvertFillGapsToTranslateController convertFillGapsToTranslateController,
        MoveCardsToDirController moveCardsToDirController,
        ValidateCardsController validateCardsController
    ) {
        return new IndexController(List.of(
            newCardController,
            exerciseController,
            convertFillGapsToTranslateController,
            moveCardsToDirController,
            validateCardsController
        ));
    }

    @Bean
    public DispatcherController dispatcherController(List<StatefulWebController<?, ?>> allControllers) {
        Map<String, StatefulWebController<?, ?>> controllerMap = allControllers.stream()
            .collect(Collectors.toMap(StatefulWebController::getId, Function.identity()));
        return new DispatcherController(controllerMap);
    }
}
