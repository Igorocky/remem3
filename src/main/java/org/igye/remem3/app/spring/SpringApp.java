package org.igye.remem3.app.spring;

import lombok.SneakyThrows;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.impl.CardUtilsImpl;
import org.igye.remem3.app.impl.SettingsImpl;
import org.igye.remem3.app.impl.ShellImpl;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.utils.impl.UtilsImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Path;
import java.util.Properties;

@SpringBootApplication
public class SpringApp {

    @SneakyThrows
    static void main(String[] args) {
        if (args.length == 1 && "-s".equals(args[0])) {
            runShell();
        } else {
            SpringApplication.run(SpringApp.class, args);
        }
    }

    @SneakyThrows
    private static void runShell() {
        Properties props = PropertiesLoaderUtils.loadProperties(
            new FileSystemResource("application.properties")
        );
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addLast(new PropertiesPropertySource("appProps", props));
        AppProps appProps = Binder.get(env).bind("app", AppProps.class).get();

        Path curDir = new File(".").toPath();
        Utils utils = new UtilsImpl(new ObjectMapper());
        Settings settings = SettingsImpl.load(appProps);
        CardUtils cardUtils = new CardUtilsImpl(utils, settings);
        ShellImpl sh = new ShellImpl(curDir, utils, cardUtils);
        sh.getReplConfig().setExprHistoryFile(new File("spel_shel_history"));
        sh.runRepl();
    }
}