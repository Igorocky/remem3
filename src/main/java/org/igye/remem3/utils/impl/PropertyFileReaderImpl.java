package org.igye.remem3.utils.impl;

import lombok.SneakyThrows;
import org.igye.remem3.app.App;
import org.igye.remem3.utils.PropertyFileReader;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

public class PropertyFileReaderImpl implements PropertyFileReader {
    private final Properties props;
    private final App app;

    @SneakyThrows
    public PropertyFileReaderImpl(App app, File propFile) {
        this.app = app;
        this.props = new Properties();
        try (FileReader fileReader = new FileReader(propFile)) {
            props.load(fileReader);
        }
    }

    @Override
    public String getPropValue(String propName) {
        String value = props.getProperty(propName);
        if (value == null) {
            return value;
        }
        if (value.startsWith("${")) {
            return app.getPropStr(value.substring(2,value.length()-1));
        }
        return value;
    }
}
