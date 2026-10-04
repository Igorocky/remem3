package org.igye.remem3.app;

import java.util.List;

public interface Settings {
    String getCacheFile();

    String getBeansFile();

    List<String> getLanguages();

    String getQuestionLanguage();

    String getAnswerLanguage();

    List<String> getDirectoriesWithCards();

    String getCardEditor();

    List<String> getPropsToPassToBeans();
}
