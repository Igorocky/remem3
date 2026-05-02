package org.igye.remem3.app.state;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import java.util.stream.Stream;

public interface StateCache {
    Optional<Object> getState(String stateId);

    void putState(String stateId, Object state);

    Stream<Pair<String, Object>> getAll();
}
