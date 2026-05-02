package org.igye.remem3.app.state.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.state.StateCache;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class StateCacheImpl implements StateCache {
    private final Clock clock;
    private final Duration evictionTimeout;
    private final Map<String, Pair<Instant, Object>> allStates = new HashMap<>();

    @Override
    public Optional<Object> getState(String stateId) {
        Pair<Instant, Object> pair = allStates.get(stateId);
        Object res = null;
        if (pair != null) {
            res = pair.getRight();
            putState(stateId, res);
        }
        deleteStaleStates();
        return Optional.ofNullable(res);
    }

    @Override
    public void putState(String stateId, Object state) {
        allStates.put(stateId, Pair.of(clock.instant(), state));
    }

    @Override
    public Stream<Pair<String, Object>> getAll() {
        return allStates.entrySet().stream().map(e -> Pair.of(e.getKey(), e.getValue().getRight()));
    }

    private void deleteStaleStates() {
        Instant maxTime = clock.instant().plus(evictionTimeout);
        List<String> statesToRemove = allStates.entrySet().stream()
            .filter(entry -> maxTime.isBefore(entry.getValue().getLeft()))
            .map(Map.Entry::getKey)
            .toList();
        statesToRemove.forEach(allStates::remove);
    }
}
