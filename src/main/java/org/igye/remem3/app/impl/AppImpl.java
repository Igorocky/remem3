package org.igye.remem3.app.impl;

import lombok.SneakyThrows;
import org.igye.remem3.app.App;
import org.igye.remem3.controllers.IndexController;
import org.igye.remem3.controllers.TextFormatController;
import org.igye.remem3.web.StatefulWebController;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AppImpl implements App {

    private final Context context;
    private final Map<String, StatefulWebController> controllers;

    @SneakyThrows
    public AppImpl() {
        this.context = InitialContext.doLookup("java:comp/env");
        Map<String, StatefulWebController> allControllers = Stream.of(
            new TextFormatController()
        ).collect(Collectors.toMap(TextFormatController::getPath, Function.identity()));
        allControllers.put(
            "",
            new IndexController(allControllers.values().stream().map(StatefulWebController::getPath).sorted().toList())
        );
        this.controllers = Collections.unmodifiableMap(allControllers);
    }

    @Override
    public String getPropStr(String propName) {
        try {
            return (String) context.lookup(propName);
        } catch (NamingException e) {
            return System.getenv(propName);
        }
    }

    @Override
    public Optional<StatefulWebController> lookupController(String path) {
        return Optional.ofNullable(controllers.get(path));
    }

    public static App getInstance() {
        return AppHolder.app;
    }

    private static final class AppHolder {
        private static final App app = new AppImpl();
    }
}
