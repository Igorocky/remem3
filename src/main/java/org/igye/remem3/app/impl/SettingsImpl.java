package org.igye.remem3.app.impl;

import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.AppProps;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.dto.BucketDelaysDto;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Utils;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Getter
@Builder
public class SettingsImpl implements Settings {
    private static final String PROP_DIRECTORIES_WITH_CARDS = "directories_with_cards";
    private static final String PROP_LANGUAGES = "languages";
    private static final String PROP_CACHE_FILE = "cache_file";
    private static final String PROP_CARD_EDITOR = "card_editor";
    private static final String PROP_BUCKET_DELAYS = "bucket_delays";
    private static final String PROP_EXERCISES = "exercises";
    private static final Pattern SPACE_PAT = Pattern.compile("\\s");

    @Builder.Default
    private List<String> languages = Collections.emptyList();
    @Builder.Default
    private List<String> directoriesWithCards = Collections.emptyList();
    @Builder.Default
    private String cacheFile = "";
    @Builder.Default
    private String cardEditor = "";
    @Builder.Default
    private List<BucketDelaysDto> bucketDelays = List.of();
    @Builder.Default
    private List<Pair<String, File>> exercises = List.of();

    public static Settings load(AppProps props, Utils utils) {
        List<String> languages = props.getLanguages();
        checkNotEmpty(languages, PROP_LANGUAGES);
        if (languages.contains("_")) {
            throw new Exn("The underscore symbol '_' cannot be used as a language name.");
        }
        for (String language : languages) {
            if (SPACE_PAT.matcher(language).find()) {
                throw new Exn("Language names cannot contains whitespaces.");
            }
        }
        List<String> directoriesWithCards = trimAndSkipEmpty(
            Collections.unmodifiableList(props.getDirectoriesWithCards())
        );
        checkNotEmpty(directoriesWithCards, PROP_DIRECTORIES_WITH_CARDS);
        String nonExistentDirs = directoriesWithCards.stream()
            .map(File::new)
            .filter(dir -> !dir.exists() || !dir.isDirectory())
            .map(File::getName)
            .collect(Collectors.joining(", "));
        if (StringUtils.isNotEmpty(nonExistentDirs)) {
            throw new Exn(String.format("Invalid directories in %s: %s", PROP_DIRECTORIES_WITH_CARDS, nonExistentDirs));
        }
        return SettingsImpl.builder()
            .languages(languages)
            .directoriesWithCards(directoriesWithCards)
            .cacheFile(checkNotBlank(props.getCacheFile(), PROP_CACHE_FILE))
            .cardEditor(checkNotBlank(props.getCardEditor(), PROP_CARD_EDITOR))
            .bucketDelays(parseBucketDelays(props.getBucketDelays(), utils))
            .exercises(loadExercises(props.getExercises()))
            .build();
    }

    @SneakyThrows
    private static List<Pair<String, File>> loadExercises(List<String> exercisesStr) {
        List<Pair<String, File>> exercises = trimAndSkipEmpty(exercisesStr).stream()
            .map(nameAndPath -> {
                String[] parts = nameAndPath.split(":");
                if (parts.length != 2) {
                    throw new Exn(String.format(
                        "Cannot parse exercise '%s', the format should be 'name:path'.", nameAndPath
                    ));
                }
                String path = parts[1].trim();
                if (StringUtils.isBlank(path)) {
                    throw new Exn(String.format(
                        "The path of an exercise must not be blank, got '%s'.", nameAndPath
                    ));
                }
                File file = new File(path);
                String name = parts[0].trim();
                if (StringUtils.isBlank(name)) {
                    name = removeExtension(file.getName());
                }
                if (StringUtils.isBlank(name)) {
                    throw new Exn(String.format(
                        "The name of an exercise must not be blank, got '%s'.", nameAndPath
                    ));
                }
                return Pair.of(name, file);
            })
            .toList();
        long namesCnt = exercises.stream().map(Pair::getLeft).distinct().count();
        if (namesCnt != exercises.size()) {
            throw new Exn("Names of all exercises must be unique.");
        }
        return exercises;
    }

    protected static String removeExtension(String name) {
        int dotIdx = name.length() - 1;
        while (dotIdx >= 0 && name.charAt(dotIdx) != '.') {
            dotIdx--;
        }
        if (dotIdx < 0) {
            return name;
        } else {
            return name.substring(0, dotIdx);
        }
    }

    private static List<BucketDelaysDto> parseBucketDelays(String str, Utils utils) {
        if (StringUtils.isBlank(str)) {
            throw new Exn(String.format("%s property cannot be empty.", PROP_BUCKET_DELAYS));
        }
        List<BucketDelaysDto> bucketDelays = Arrays.stream(str.split(";"))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .map(part -> {
                String[] nameAndDelays = part.split(":");
                if (nameAndDelays.length != 2) {
                    throw new Exn(String.format(
                        "Each bucket delays part must consist of a name and delays; cannot parse '%s'.", part
                    ));
                }
                String name = nameAndDelays[0].trim();
                if (StringUtils.isBlank(name)) {
                    throw new Exn(String.format("The name of bucket delays part must not be empty, got '%s'.", part));
                }
                String delaysFromProps = nameAndDelays[1].trim();
                List<Duration> delays = utils.parseDurations(delaysFromProps);
                if (delays.isEmpty()) {
                    throw new Exn(String.format("At least one bucket must be defined, got 0 in '%s'.", part));
                }
                return BucketDelaysDto.builder()
                    .name(name)
                    .delays(delays)
                    .delaysFromProps(delaysFromProps)
                    .build();
            })
            .toList();
        long distinctNameCnt = bucketDelays.stream()
            .map(BucketDelaysDto::getName)
            .distinct()
            .count();
        if (bucketDelays.size() != distinctNameCnt) {
            throw new Exn("All bucket delays names must be unique");
        }
        return bucketDelays;
    }

    private static void checkNotEmpty(List<String> values, String propName) {
        if (CollectionUtils.isEmpty(values)) {
            throw new Exn(String.format("Property '%s' is empty", propName));
        }
    }

    private static String checkNotBlank(String value, String propName) {
        if (StringUtils.isBlank(value)) {
            throw new Exn(String.format("The '%s' property must not be blank.", propName));
        }
        return value;
    }

    private static List<String> trimAndSkipEmpty(List<String> list) {
        return list.stream()
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .toList();
    }
}
