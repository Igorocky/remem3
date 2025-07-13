package org.igye.remem3.utils.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.igye.remem3.utils.Utils;

import java.io.File;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class UtilsImpl implements Utils {
    private final ObjectMapper objectMapper;

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

    @SneakyThrows
    @Override
    public <T> T parseJson(String jsonStr, Class<T> clazz) {
        return objectMapper.readValue(jsonStr, clazz);
    }

    @SneakyThrows
    @Override
    public String objToJson(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }

    @Override
    public String makeExpectedActualPair(String expected, String actual) {
        return "###EXP " + expected + " ###ACT " + actual;
    }
}
