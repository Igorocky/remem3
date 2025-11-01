package org.igye.remem3.app.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.igye.remem3.app.AppProps;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.impl.CacheImpl;
import org.igye.remem3.app.impl.CardUtilsImpl;
import org.igye.remem3.app.impl.SettingsImpl;
import org.igye.remem3.controllers.IndexController;
import org.igye.remem3.controllers.exercise.ExerciseController;
import org.igye.remem3.controllers.newcard.NewCardController;
import org.igye.remem3.controllers.validatecards.ValidateCardsController;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.utils.impl.UtilsImpl;
import org.igye.remem3.web.DispatcherController;
import org.igye.remem3.web.StatefulWebController;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableConfigurationProperties(AppProps.class)
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
    public Utils utils(ObjectMapper objectMapper) {
        return new UtilsImpl(objectMapper);
    }

    @Bean
    public CardUtils cardUtils(Utils utils, Settings settings) {
        return new CardUtilsImpl(utils, settings);
    }

    @Bean
    public Cache cache(Settings settings, Utils utils) {
        return new CacheImpl(utils, new File(settings.getCacheFile()));
    }

    @Bean
    public NewCardController newCardController(Settings settings, Cache cache, Utils utils) {
        return new NewCardController(settings, cache, utils);
    }

    @Bean
    public ExerciseController exerciseController(Clock clock, Settings settings, Cache cache, Utils utils) {
        return new ExerciseController(clock, settings, cache, utils);
    }

    @Bean
    public ValidateCardsController validateCardsController(CardUtils cardUtils) {
        return new ValidateCardsController(cardUtils);
    }

    @Bean
    public IndexController indexController(
        NewCardController newCardController,
        ExerciseController exerciseController,
        ValidateCardsController validateCardsController
    ) {
        return new IndexController(List.of(newCardController, exerciseController, validateCardsController));
    }

    @Bean
    public DispatcherController dispatcherController(
        NewCardController newCardController,
        ExerciseController exerciseController,
        ValidateCardsController validateCardsController,
        IndexController indexController
    ) {
        Map<String, StatefulWebController<?, ?>> controllers = Stream.of(
            newCardController,
            exerciseController,
            validateCardsController,
            indexController
        ).collect(Collectors.toMap(StatefulWebController::getId, Function.identity()));
        return new DispatcherController(controllers);
    }
}
