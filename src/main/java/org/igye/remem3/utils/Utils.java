package org.igye.remem3.utils;

import java.io.File;

public interface Utils {
    String readStringFromFile(File file);

    void writeStringToFile(String str, File file);
}
