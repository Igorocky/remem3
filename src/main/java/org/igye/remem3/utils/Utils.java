package org.igye.remem3.utils;

import java.io.File;
import java.util.List;

public interface Utils {
    String readFileToString(File file);

    List<String> readLines(File file);
}
