package org.igye.remem3.app;

import java.util.List;

public interface Settings {
    String getCacheFile();

    List<String> getLanguages();

    List<String> getDirectoriesWithCards();

    String getCardEditor();
}
