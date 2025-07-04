package org.igye.remem3.utils.impl;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.igye.remem3.utils.Utils;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class UtilsImpl implements Utils {
    @SneakyThrows
    @Override
    public String readStringFromFile(File file) {
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    @SneakyThrows
    @Override
    public void writeStringToFile(String str, File file) {
        FileUtils.writeStringToFile(file, str, StandardCharsets.UTF_8);
    }
}
