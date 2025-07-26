package org.igye.remem3.utils.impl;

import lombok.SneakyThrows;
import org.igye.remem3.app.App;
import org.igye.remem3.utils.PropertyFileReader;
import org.igye.remem3.utils.Utils;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

public class PropertyFileReaderImpl implements PropertyFileReader {
    private final App app;
    private final Utils utils;
    private final String thisFileDir;
    private final Properties props;

    @SneakyThrows
    public PropertyFileReaderImpl(App app, File propFile) {
        this.app = app;
        this.utils = app.getUtils();
        this.thisFileDir = propFile.getParentFile().getAbsolutePath();
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
        if (value.contains("${")) {
            return utils.replacePlaceholders(value, ph -> {
                if ("this_file_dir".equals(ph)) {
                    return thisFileDir;
                } else {
                    return app.getPropStr(ph);
                }
            });
        }
        return value;
    }
}
