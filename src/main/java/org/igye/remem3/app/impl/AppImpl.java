package org.igye.remem3.app.impl;

import lombok.SneakyThrows;
import org.apache.commons.dbcp2.BasicDataSource;
import org.igye.remem3.app.App;
import org.igye.remem3.controllers.IndexController;
import org.igye.remem3.controllers.TextFormatController;
import org.igye.remem3.utils.PropertyFileReader;
import org.igye.remem3.utils.impl.PropertyFileReaderImpl;
import org.igye.remem3.web.StatefulWebController;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AppImpl implements App {

    private final Context context;
    private final List<PropertyFileReader> propFiles = new ArrayList<>();
    private final Map<String, StatefulWebController> controllers;
    private final BasicDataSource dataSource;

    @SneakyThrows
    public AppImpl() {
        this.context = InitialContext.doLookup("java:comp/env");
        propFiles.addAll(
            getPropList("property-files", Collections.emptyList()).stream()
                .map(File::new)
                .map(propFile -> new PropertyFileReaderImpl(this, propFile))
                .toList()
        );
        Map<String, StatefulWebController> allControllers = Stream.of(
            new TextFormatController()
        ).collect(Collectors.toMap(TextFormatController::getPath, Function.identity()));
        allControllers.put(
            "",
            new IndexController(allControllers.values().stream().map(StatefulWebController::getPath).sorted().toList())
        );
        this.controllers = Collections.unmodifiableMap(allControllers);
        dataSource = makeDataSource("dataSource");
    }

    @Override
    public String getPropStr(String propName) {
        return getPropStr(propName, null);
    }

    @Override
    public String getPropStr(String propName, String defaultValue) {
        try {
            return String.valueOf(context.lookup(propName));
        } catch (NamingException e) {
            String envPropVal = System.getenv(propName);
            if (envPropVal != null) {
                return envPropVal;
            }
            return propFiles.stream()
                .map(props -> props.getPropValue(propName))
                .filter(Objects::nonNull)
                .findFirst().orElse(defaultValue);
        }
    }

    @Override
    public Long getPropLong(String propName) {
        return getPropLong(propName, null);
    }

    @Override
    public Long getPropLong(String propName, Long defaultValue) {
        String propStr = getPropStr(propName);
        if (propStr == null) {
            return defaultValue;
        }
        return Long.parseLong(propStr);
    }

    @Override
    public Integer getPropInt(String propName) {
        return getPropInt(propName, null);
    }

    @Override
    public Integer getPropInt(String propName, Integer defaultValue) {
        String propStr = getPropStr(propName);
        if (propStr == null) {
            return defaultValue;
        }
        return Integer.parseInt(propStr);
    }

    @Override
    public List<String> getPropList(String propName) {
        return getPropList(propName, null);
    }

    @Override
    public List<String> getPropList(String propName, List<String> defaultValue) {
        String propStr = getPropStr(propName);
        if (propStr == null) {
            return defaultValue;
        }
        return Arrays.stream(propStr.split(",")).toList();
    }

    @Override
    public Optional<StatefulWebController> lookupController(String path) {
        return Optional.ofNullable(controllers.get(path));
    }

    @Override
    public BasicDataSource getDataSource() {
        return dataSource;
    }

    public static App getInstance() {
        return AppHolder.app;
    }

    private BasicDataSource makeDataSource(String prefix) {
        prefix += ".";
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("org.sqlite.JDBC");
        ds.setUsername(getPropStr(prefix + "username", ""));
        ds.setPassword(getPropStr(prefix + "password", ""));
        ds.setUrl(getPropStr(prefix + "url"));
        ds.setMaxTotal(getPropInt(prefix + "maxTotal", 5));
        ds.setMaxIdle(getPropInt(prefix + "maxIdle", 5));
        ds.setInitialSize(getPropInt(prefix + "initialSize", 5));
        ds.setValidationQuery(getPropStr(prefix + "validationQuery", "select 1"));
        return ds;
    }

    private static final class AppHolder {
        private static final App app = new AppImpl();
    }
}
