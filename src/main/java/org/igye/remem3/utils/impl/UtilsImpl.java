package org.igye.remem3.utils.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Utils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class UtilsImpl implements Utils {
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhdM])");
    private static final Map<String, Long> UNIT_TO_SECONDS = Map.of(
        "s", 1L,
        "m", 60L,
        "h", 60L * 60,
        "d", 60L * 60 * 24
    );
    private static final List<Pair<Long, String>> SECONDS_TO_UNIT = UNIT_TO_SECONDS.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .map(e -> Pair.of(e.getValue(), e.getKey()))
        .toList();
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    public String readStringFromFile(File file) {
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    @SneakyThrows
    @Override
    public void writeStringToFile(String str, File file) {
        FileUtils.writeStringToFile(file, str, StandardCharsets.UTF_8);
    }

    @SneakyThrows
    @Override
    public <T> T parseJson(String jsonStr, Class<T> clazz) {
        return objectMapper.readValue(jsonStr, clazz);
    }

    @SneakyThrows
    @Override
    public String objToJson(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }

    @Override
    public String makeExpectedActualPair(String expected, String actual) {
        return "###EXP " + expected + " ###ACT " + actual;
    }

    @Override
    public Duration parseDuration(String durStr) {
        if (StringUtils.isBlank(durStr)) {
            throw new Exn("Duration cannot be empty");
        }
        return Arrays.stream(durStr.trim().split("\\s+"))
            .map(this::parseSingleDuration)
            .reduce(Duration::plus)
            .orElseThrow(() -> new Exn("Cannot get the sum of durations."));
    }

    @Override
    public String durationToStr(Duration dur) {
        StringBuilder sb = new StringBuilder();
        long seconds = dur.getSeconds();
        Iterator<Pair<Long, String>> iter = SECONDS_TO_UNIT.iterator();
        while (seconds > 0) {
            Pair<Long, String> unit = iter.next();
            long units = seconds / unit.getLeft();
            if (units > 0) {
                sb.append(" ").append(units).append(unit.getRight());
                seconds = seconds % unit.getLeft();
            }
        }
        if (sb.isEmpty()) {
            return "0s";
        }
        return sb.toString().trim();
    }

    private Duration parseSingleDuration(String str) {
        Matcher matcher = DURATION_PATTERN.matcher(str);
        if (!matcher.matches()) {
            throw new Exn(String.format("Cannot parse duration '%s'.", str));
        }
        return Duration.ofSeconds(Long.parseLong(matcher.group(1)) * UNIT_TO_SECONDS.get(matcher.group(2)));
    }
}
