package org.igye.remem3.app.spring;

import org.igye.remem3.app.impl.ShellImpl;
import org.igye.remem3.utils.impl.UtilsImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tools.jackson.databind.ObjectMapper;

import java.io.File;

@SpringBootApplication
public class SpringApp {

    static void main(String[] args) {
        for (String arg : args) {
            if ("-s".equals(arg)) {
                ShellImpl sh = new ShellImpl(new File(".").toPath(), new UtilsImpl(new ObjectMapper()));
                sh.getReplConfig().setExprHistoryFile(new File("spel_shel_history"));
                sh.runRepl();
                return;
            }
        }
        SpringApplication.run(SpringApp.class, args);
    }
}