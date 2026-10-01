package org.igye.remem3.app;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface Shell {
    List<Pair<String, Object>> getBeans();

    <T> List<Pair<String, T>> getBeans(Class<T> type);

    <T> Pair<String, T> getBean(Class<T> type);
}
