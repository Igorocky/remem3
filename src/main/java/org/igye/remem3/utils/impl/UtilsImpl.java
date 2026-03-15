package org.igye.remem3.utils.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.repeatstrategy.HistRec;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Producer;
import org.igye.remem3.utils.Utils;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

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
    private static final Pattern PLACEHOLDER_PAT = Pattern.compile("\\$\\{([a-zA-Z0-9_-]+)}");
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
    public List<Duration> parseDurations(String durStr) {
        return Arrays.stream(durStr.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .map(this::parseDuration)
            .toList();
    }

    @Override
    public String durationToStr(Duration dur, int precision) {
        if (precision <= 0) {
            throw new Exn("durationToStr: precision must be a positive integer, but got %s.".formatted(precision));
        }
        StringBuilder sb = new StringBuilder();
        long seconds = dur.getSeconds();
        Iterator<Pair<Long, String>> iter = SECONDS_TO_UNIT.iterator();
        while (seconds > 0 && precision > 0) {
            Pair<Long, String> unit = iter.next();
            long units = seconds / unit.getLeft();
            if (units > 0) {
                precision--;
                sb.append(" ").append(units).append(unit.getRight());
                seconds = seconds % unit.getLeft();
            }
        }
        if (sb.isEmpty()) {
            return "0s";
        }
        return sb.toString().trim();
    }

    @Override
    public String durationToStr(Duration duration) {
        return durationToStr(duration, 10);
    }

    @Override
    public String replacePlaceholders(String text, Function<String, String> valueSupplier) {
        StringBuilder res = new StringBuilder();
        int lastIdx = 0;
        Matcher matcher = PLACEHOLDER_PAT.matcher(text);
        while (matcher.find()) {
            res.append(text, lastIdx, matcher.start());
            res.append(valueSupplier.apply(matcher.group(1)));
            lastIdx = matcher.end();
        }
        res.append(text, lastIdx, text.length());
        return res.toString();
    }

    @Override
    public int getInRange(int min, int value, int max) {
        return Math.max(min, Math.min(value, max));
    }

    @Override
    public long getInRange(long min, long value, long max) {
        return Math.max(min, Math.min(value, max));
    }

    @Override
    public double getInRange(double min, double value, double max) {
        return Math.max(min, Math.min(value, max));
    }

    @Override
    public <T> Optional<T> try_(Producer<T> producer) {
        try {
            return Optional.of(producer.get());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public int getStreak(List<HistRec> hist) {
        return getStreak(hist, Instant.MIN);
    }

    @Override
    public int getStreak(List<HistRec> hist, int maxStreak) {
        return Math.min(getStreak(hist), maxStreak);
    }

    @Override
    public int getStreak(List<HistRec> hist, Instant startTime) {
        int res = 0;
        for (int i = hist.size() - 1; i >= 0; i--) {
            HistRec rec = hist.get(i);
            if (rec.getTime().isBefore(startTime) || !rec.isPassed()) {
                break;
            }
            res++;
        }
        return res;
    }

    @Override
    public BigDecimal calOverdue(BigDecimal minDelay, BigDecimal actualDelay) {
        return actualDelay.subtract(minDelay).divide(minDelay, RoundingMode.HALF_UP);
    }

    @Override
    public <E, V> Pair<V, V> getMinMax(List<E> elems, Function<E, V> prop, Comparator<V> cmp, Pair<V, V> dflt) {
        if (CollectionUtils.isEmpty(elems)) {
            return dflt;
        }
        V min = null;
        V max = null;
        for (E elem : elems) {
            V val = prop.apply(elem);
            if (min == null || cmp.compare(val, min) < 0) {
                min = val;
            }
            if (max == null || cmp.compare(max, val) < 0) {
                max = val;
            }
        }
        return Pair.of(min, max);
    }

    private Duration parseSingleDuration(String str) {
        Matcher matcher = DURATION_PATTERN.matcher(str);
        if (!matcher.matches()) {
            throw new Exn(format("Cannot parse duration '%s'.", str));
        }
        return Duration.ofSeconds(Long.parseLong(matcher.group(1)) * UNIT_TO_SECONDS.get(matcher.group(2)));
    }
}
