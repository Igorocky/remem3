package org.igye.remem3.app.impl;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.igye.remem3.app.App;
import org.igye.remem3.app.Settings;
import org.igye.remem3.utils.Exn;

import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class SettingsImpl implements Settings {
    private static final String PROP_DIRECTORIES_WITH_CARDS = "directories_with_cards";
    private static final String PROP_LANGUAGES = "languages";

    @Builder.Default
    private List<String> languages = Collections.emptyList();
    @Builder.Default
    private List<String> directoriesWithCards = Collections.emptyList();
    @Builder.Default
    private String cacheFile = "";

    public static Settings load(App app) {
        List<String> languages = trimAndSkipEmpty(Collections.unmodifiableList(app.getPropList(PROP_LANGUAGES)));
        checkNotEmpty(languages, PROP_LANGUAGES);
        if (languages.contains("_")) {
            throw new Exn("The underscore symbol '_' cannot be used as a language name.");
        }
        List<String> directoriesWithCards = trimAndSkipEmpty(
            Collections.unmodifiableList(app.getPropList(PROP_DIRECTORIES_WITH_CARDS))
        );
        checkNotEmpty(directoriesWithCards, PROP_DIRECTORIES_WITH_CARDS);
        return SettingsImpl.builder()
            .languages(languages)
            .directoriesWithCards(directoriesWithCards)
            .cacheFile(app.getPropStr("cache_file"))
            .build();
    }

    private static void checkNotEmpty(List<String> values, String propName) {
        if (CollectionUtils.isEmpty(values)) {
            throw new Exn(String.format("Property '%s' is empty", propName));
        }
    }

    private static List<String> trimAndSkipEmpty(List<String> list) {
        return list.stream()
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .toList();
    }
}
