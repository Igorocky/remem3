package org.igye.remem3.utils;

import java.io.File;

public interface Utils {
    String readStringFromFile(File file);

    void writeStringToFile(String str, File file);

    <T> T parseJson(String jsonStr, Class<T> clazz);

    String objToJson(Object obj);

    String makeExpectedActualPair(String expected, String actual);
}
