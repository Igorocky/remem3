package org.igye.remem3.app.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.dbcp2.BasicDataSource;
import org.igye.remem3.app.App;
import org.igye.remem3.app.db.RememDbSchema;
import org.igye.remem3.app.db.entities.CardEnt;
import org.igye.remem3.app.db.entities.CardHistEnt;
import org.igye.remem3.app.db.entities.CardTypeEnt;
import org.igye.remem3.app.db.entities.FolderEnt;
import org.igye.remem3.app.db.entities.LangEnt;
import org.igye.remem3.app.db.impl.RememDbSchemaImpl;
import org.igye.remem3.app.manager.explorer.Explorer;
import org.igye.remem3.app.manager.explorer.impl.ExplorerImpl;
import org.igye.remem3.app.manager.language.LangManager;
import org.igye.remem3.app.manager.language.impl.LangManagerImpl;
import org.igye.remem3.controllers.DbAccessController;
import org.igye.remem3.controllers.ExplorerController;
import org.igye.remem3.controllers.IndexController;
import org.igye.remem3.controllers.LangController;
import org.igye.remem3.controllers.TextFormatController;
import org.igye.remem3.controllers.newcard.NewCardController;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.PropertyFileReader;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.utils.impl.PropertyFileReaderImpl;
import org.igye.remem3.utils.impl.UtilsImpl;
import org.igye.remem3.utils.sqlite.Database;
import org.igye.remem3.utils.sqlite.impl.DatabaseImpl;
import org.igye.remem3.web.StatefulWebController;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AppImpl implements App {

    private Utils utils;
    private final Context context;
    private final List<PropertyFileReader> propFiles = new ArrayList<>();
    private final RememDbSchema dbSchema;
    private final Database database;
    private final Map<String, StatefulWebController> controllers;

    public static App getInstance() {
        return AppHolder.app;
    }

    @SneakyThrows
    public AppImpl() {
        this.utils = new UtilsImpl(new ObjectMapper());
        this.context = InitialContext.doLookup("java:comp/env");
        reloadProperties();

        this.dbSchema = new RememDbSchemaImpl();
        this.database = new DatabaseImpl(makeDataSource("dataSource"), dbSchema);
        database.registerTableForEntity(LangEnt.class, dbSchema.getLanguageTable());
        database.registerTableForEntity(FolderEnt.class, dbSchema.getFolderTable());
        database.registerTableForEntity(CardTypeEnt.class, dbSchema.getCardTypeTable());
        database.registerTableForEntity(CardEnt.class, dbSchema.getCardTable());
        database.registerTableForEntity(CardHistEnt.class, dbSchema.getCardTable().getHistTable());

        LangManager langManager = new LangManagerImpl(database);
        Explorer explorer = new ExplorerImpl(database);
        Map<String, StatefulWebController> allControllers = Stream.of(
            new TextFormatController(),
            new LangController(langManager),
            new ExplorerController(explorer),
            new NewCardController(),
            new DbAccessController(database)
        ).collect(Collectors.toMap(StatefulWebController::getPath, Function.identity()));
        allControllers.put(
            "",
            new IndexController(allControllers.values().stream().map(StatefulWebController::getPath).sorted().toList())
        );
        this.controllers = Collections.unmodifiableMap(allControllers);
    }

    @Override
    public void reloadProperties() {
        propFiles.clear();
        propFiles.addAll(
            getPropList("property-files", Collections.emptyList()).stream()
                .map(File::new)
                .map(propFile -> new PropertyFileReaderImpl(this, propFile))
                .toList()
        );
    }

    @Override
    public String getPropStr(String propName, String defaultValue) {
        String prop = getProp(propName);
        return prop != null ? prop : defaultValue;
    }

    @Override
    public String getPropStr(String propName) {
        String prop = getProp(propName);
        if (prop == null) {
            throwPropIsNotSet(propName);
        }
        return prop;
    }

    @Override
    public Long getPropLong(String propName, Long defaultValue) {
        String propStr = getProp(propName);
        if (propStr == null) {
            return defaultValue;
        }
        return Long.parseLong(propStr);
    }

    @Override
    public Long getPropLong(String propName) {
        String propStr = getProp(propName);
        if (propStr == null) {
            throwPropIsNotSet(propName);
        }
        return Long.parseLong(propStr);
    }

    @Override
    public Integer getPropInt(String propName, Integer defaultValue) {
        String propStr = getProp(propName);
        if (propStr == null) {
            return defaultValue;
        }
        return Integer.parseInt(propStr);
    }

    @Override
    public Integer getPropInt(String propName) {
        String propStr = getProp(propName);
        if (propStr == null) {
            throwPropIsNotSet(propName);
        }
        return Integer.parseInt(propStr);
    }

    @Override
    public List<String> getPropList(String propName, List<String> defaultValue) {
        String propStr = getProp(propName);
        if (propStr == null) {
            return defaultValue;
        }
        return Arrays.stream(propStr.split(",")).toList();
    }

    @Override
    public List<String> getPropList(String propName) {
        String propStr = getProp(propName);
        if (propStr == null) {
            throwPropIsNotSet(propName);
        }
        return Arrays.stream(propStr.split(",")).toList();
    }

    @Override
    public Optional<StatefulWebController> lookupController(String path) {
        return Optional.ofNullable(controllers.get(path));
    }

    @Override
    public Database getDatabase() {
        return database;
    }

    @Override
    public Utils getUtils() {
        return this.utils;
    }

    private String getProp(String propName) {
        try {
            Object valueFromContext = context.lookup(propName);
            if (valueFromContext == null) {
                throw new NamingException();
            }
            return String.valueOf(valueFromContext);
        } catch (NamingException e) {
            String envPropVal = System.getenv(propName);
            if (envPropVal != null) {
                return envPropVal;
            }
            return propFiles.stream()
                .map(props -> props.getPropValue(propName))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        }
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
        ds.setDefaultAutoCommit(true);
        ds.setAutoCommitOnReturn(true);
        return ds;
    }

    private static void throwPropIsNotSet(String propName) {
        throw new Exn(String.format("Property '%s' is not set.", propName));
    }

    private static final class AppHolder {
        private static final App app = new AppImpl();
    }
}
