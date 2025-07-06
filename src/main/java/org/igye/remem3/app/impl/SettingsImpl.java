package org.igye.remem3.app.impl;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.igye.remem3.app.App;
import org.igye.remem3.app.Settings;

import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class SettingsImpl implements Settings {
    public static final String PROP_DIRECTORIES_WITH_CARDS = "directories_with_cards";
    public static final String PROP_LANGUAGES = "languages";

    @Builder.Default
    private List<String> languages = Collections.emptyList();
    @Builder.Default
    private List<String> directoriesWithCards = Collections.emptyList();
    @Builder.Default
    private String cacheFile = "";

    public static Settings load(App app) {
        return SettingsImpl.builder()
            .languages(trimAndSkipEmpty(
                Collections.unmodifiableList(app.getPropList(PROP_LANGUAGES))
            ))
            .directoriesWithCards(trimAndSkipEmpty(
                Collections.unmodifiableList(app.getPropList(PROP_DIRECTORIES_WITH_CARDS))
            ))
            .cacheFile(app.getPropStr("cache_file"))
            .build();
    }

    private static List<String> trimAndSkipEmpty(List<String> list) {
        return list.stream()
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .toList();
    }
}
