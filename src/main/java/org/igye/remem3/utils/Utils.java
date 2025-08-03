package org.igye.remem3.utils;

import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.repeatstrategy.HistRec;

import java.io.File;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface Utils {
    String readStringFromFile(File file);

    void writeStringToFile(String str, File file);

    <T> T parseJson(String jsonStr, Class<T> clazz);

    String objToJson(Object obj);

    String makeExpectedActualPair(String expected, String actual);

    Duration parseDuration(String durStr);

    List<Duration> parseDurations(String durStr);

    String durationToStr(Duration duration);

    String replacePlaceholders(String text, Function<String, String> valueSupplier);

    int getInRange(int min, int value, int max);

    long getInRange(long min, long value, long max);

    double getInRange(double min, double value, double max);

    <T> Optional<T> try_(Producer<T> producer);

    int getStreak(List<HistRec> hist);

    int getStreak(List<HistRec> hist, int maxStreak);

    int getStreak(List<HistRec> hist, Instant startTime);

    BigDecimal calOverdue(BigDecimal minDelay, BigDecimal actualDelay);

    <E, V> Pair<V, V> getMinMax(List<E> elems, Function<E, V> prop, Comparator<V> cmp, Pair<V, V> dflt);
}
