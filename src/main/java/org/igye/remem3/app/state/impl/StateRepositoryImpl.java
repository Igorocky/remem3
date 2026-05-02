package org.igye.remem3.app.state.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.state.StateCache;
import org.igye.remem3.app.state.StateConstructor;
import org.igye.remem3.app.state.StateRenderer;
import org.igye.remem3.app.state.StateRepository;
import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.app.state.TypeSupporter;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.web.RequestParams;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StateRepositoryImpl implements StateRepository {

    private final StateCache stateCache;
    private final Map<String, StateConstructor> stateConstructors;
    private final List<StateUpdater<?, ?>> stateUpdaters;
    private final List<StateRenderer<?>> stateRenderers;

    public StateRepositoryImpl(
        Clock clock,
        Duration evictionTimeout,
        List<StateConstructor> stateConstructors,
        List<StateUpdater<?, ?>> stateUpdaters,
        List<StateRenderer<?>> stateRenderers
    ) {
        this.stateCache = new StateCacheImpl(clock, evictionTimeout);
        this.stateConstructors = stateConstructors.stream().collect(Collectors.toMap(
            StateConstructor::getName, Function.identity()
        ));
        this.stateUpdaters = stateUpdaters;
        this.stateRenderers = stateRenderers;
    }

    @Override
    public String getActualStateId(String stateId) {
        return loadState(stateId).getLeft();
    }

    @Override
    public void updateState(String stateId, RequestParams params) {
        Pair<String, Object> idAndState = loadState(stateId);
        Object state = idAndState.getRight();
        StateUpdater<Object, Object> stateUpdater = (StateUpdater<Object, Object>) findTypeSupporter(
            stateUpdaters, state.getClass(), "state updater"
        );
        String actualStateId = idAndState.getLeft();
        stateCache.putState(actualStateId, stateUpdater.update(state, params));
    }

    @Override
    public String rednerState(String stateId) {
        Object state = loadState(stateId).getRight();
        StateRenderer<Object> stateRenderer = (StateRenderer<Object>) findTypeSupporter(
            stateRenderers, state.getClass(), "state renderer"
        );
        return stateRenderer.render(state);
    }

    private Pair<String, Object> loadState(String stateId) {
        Optional<?> stateOpt = stateCache.getState(stateId);
        Pair<String, Object> idAndState;
        if (stateOpt.isEmpty()) {
            StateConstructor<?> constructor = findConstructor(stateId);
            if (constructor.isSingleton()) {
                Class<?> type = constructor.getSupportedType();
                idAndState = stateCache.getAll()
                    .filter(st -> type.isAssignableFrom(st.getRight().getClass()))
                    .findFirst()
                    .orElse(makeNewState(constructor));
            } else {
                idAndState = makeNewState(constructor);
            }
        } else {
            idAndState = Pair.of(stateId, stateOpt.get());
        }
        String actualStateId = idAndState.getLeft();
        if (!actualStateId.equals(stateId)) {
            Object createdState = idAndState.getRight();
            stateCache.putState(actualStateId, createdState);
        }
        return idAndState;
    }

    private Pair<String, Object> makeNewState(StateConstructor<?> constructor) {
        return Pair.of(
            UUID.randomUUID().toString().replace("-", ""),
            constructor.construct()
        );
    }

    private StateConstructor<?> findConstructor(String stateId) {
        StateConstructor<?> constructor = stateConstructors.get(stateId);
        if (constructor == null) {
            throw new Exn("Cannot find a state constructor for '%s'.".formatted(stateId));
        }
        return constructor;
    }

    private <T extends TypeSupporter> T findTypeSupporter(List<T> typeSupporters, Class<?> type, String elemType) {
        return typeSupporters.stream()
            .filter(ts -> type.isAssignableFrom(ts.getSupportedType()))
            .findFirst()
            .orElseThrow(() -> new Exn("Cannot find a %s for %s.".formatted(elemType, type)));
    }
}
