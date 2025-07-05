package org.igye.remem3.app;

import java.util.List;

public interface RememSettings {
    String getCacheFile();

    List<String> getLanguages();

    List<String> getDirectoriesWithCards();
}
