package org.igye.remem3.app.controllers.beans;

import lombok.RequiredArgsConstructor;
import org.devtoolsgroup.simplespelshell.SpelEvaluator;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers.beans.converter.DurationConverter;
import org.igye.remem3.app.controllers.beans.converter.InstantConverter;
import org.igye.remem3.app.controllers.beans.converter.OperatorOverloaderImpl;
import org.igye.remem3.app.controllers.beans.converter.TaskFilterConverter;
import org.igye.remem3.app.controllers.beans.converter.TaskTypeMatcherConverter;
import org.igye.remem3.app.impl.ShellImpl;
import org.igye.remem3.app.state.StateConstructor;
import org.igye.remem3.utils.Utils;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
@Order(1000)
public class BeansConstructor implements StateConstructor<BeansState> {
    public static final String BEANS = "beans";
    private final Settings settings;
    private final Environment environment;
    private final Utils utils;

    @Override
    public String getName() {
        return BEANS;
    }

    @Override
    public String getDisplayName() {
        return "Beans";
    }

    @Override
    public BeansState construct() {
        Path appBaseDir = Path.of(Objects.requireNonNull(environment.getProperty("app.base-dir")));
        ShellImpl sh = new ShellImpl(appBaseDir);

        SpelEvaluator spelEvaluator = sh.getSpelEvaluator();
        List<Converter<?, ?>> typeConverters = new ArrayList<>(spelEvaluator.getTypeConverters());
        typeConverters.add(new InstantConverter());
        typeConverters.add(new TaskTypeMatcherConverter());
        typeConverters.add(new TaskFilterConverter(sh));
        typeConverters.add(new DurationConverter(utils));
        spelEvaluator.setTypeConverters(typeConverters);
        spelEvaluator.setOperatorOverloader(new OperatorOverloaderImpl(spelEvaluator.getConversionService()));

        Map<String, Object> propsToPassToBeans = new HashMap<>();
        for (String propToPassToBeans : settings.getPropsToPassToBeans()) {
            propsToPassToBeans.put(propToPassToBeans, environment.getProperty(propToPassToBeans, Object.class));
        }
        sh.var("env", propsToPassToBeans);

        sh.runScript(Path.of(settings.getBeansFile()));
        return new BeansState(sh);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
