package org.igye.remem3.app.impl;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.igye.remem3.app.App;
import org.igye.remem3.app.Settings;
import org.igye.remem3.utils.Exn;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class SettingsImpl implements Settings {
    private static final String PROP_DIRECTORIES_WITH_CARDS = "directories_with_cards";
    private static final String PROP_LANGUAGES = "languages";
    private static final String PROP_CACHE_FILE = "cache_file";
    private static final String PROP_CARD_EDITOR = "card_editor";

    @Builder.Default
    private List<String> languages = Collections.emptyList();
    @Builder.Default
    private List<String> directoriesWithCards = Collections.emptyList();
    @Builder.Default
    private String cacheFile = "";
    @Builder.Default
    private String cardEditor = "";

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
        String nonExistentDirs = directoriesWithCards.stream()
            .map(path -> new File(path))
            .filter(dir -> !dir.exists() || !dir.isDirectory())
            .map(File::getName)
            .collect(Collectors.joining(", "));
        if (StringUtils.isNotEmpty(nonExistentDirs)) {
            throw new Exn(String.format("Invalid directories in %s: %s", PROP_DIRECTORIES_WITH_CARDS, nonExistentDirs));
        }
        return SettingsImpl.builder()
            .languages(languages)
            .directoriesWithCards(directoriesWithCards)
            .cacheFile(getNotBlankProp(app, PROP_CACHE_FILE))
            .cardEditor(getNotBlankProp(app, PROP_CARD_EDITOR))
            .build();
    }

    private static String getNotBlankProp(App app, String propName) {
        String value = app.getPropStr(propName);
        checkNotBlank(value, propName);
        return value;
    }

    private static void checkNotEmpty(List<String> values, String propName) {
        if (CollectionUtils.isEmpty(values)) {
            throw new Exn(String.format("Property '%s' is empty", propName));
        }
    }

    private static void checkNotBlank(String value, String propName) {
        if (StringUtils.isBlank(value)) {
            throw new Exn(String.format("The '%s' property must not be blank.", propName));
        }
    }

    private static List<String> trimAndSkipEmpty(List<String> list) {
        return list.stream()
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .toList();
    }
}
