package org.igye.remem3.app.state.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.state.StateCache;
import org.igye.remem3.app.state.StateConstructor;
import org.igye.remem3.app.state.StateRenderer;
import org.igye.remem3.app.state.StateRepository;
import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.web.RequestParams;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StateRepositoryImpl implements StateRepository {

    private final StateCache stateCache;
    private final Map<String, StateConstructor<?>> nameToConstructor;
    private final List<Pair<Class<?>, StateUpdater<?>>> stateUpdaters;
    private final List<Pair<Class<?>, StateRenderer<?>>> stateRenderers;

    public StateRepositoryImpl(
        Clock clock,
        Duration evictionTimeout,
        List<StateConstructor<?>> stateConstructors,
        List<StateUpdater<?>> stateUpdaters,
        List<StateRenderer<?>> stateRenderers
    ) {
        this.stateCache = new StateCacheImpl(clock, evictionTimeout);
        this.nameToConstructor = stateConstructors.stream().collect(Collectors.toMap(
            StateConstructor::getName, Function.identity()
        ));
        this.stateUpdaters = determineSupportedTypes(stateUpdaters, StateUpdater.class);
        this.stateRenderers = determineSupportedTypes(stateRenderers, StateRenderer.class);
    }

    @Override
    public String getActualStateId(String stateId) {
        return loadState(stateId).getLeft();
    }

    @Override
    public void updateState(String stateId, RequestParams params) {
        Pair<String, Object> idAndState = loadState(stateId);
        Object state = idAndState.getRight();
        StateUpdater<Object> stateUpdater = (StateUpdater<Object>) findTypeSupporter(
            stateUpdaters, state.getClass(), "state updater"
        );
        String actualStateId = idAndState.getLeft();
        Object newState = stateUpdater.update(state, params);
        if (newState == null) {
            throw new Exn("%s returned null new state.".formatted(stateUpdater));
        }
        stateCache.putState(actualStateId, newState);
    }

    @Override
    public HtmlElem rednerState(String stateId) {
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
                Class<?> type = getSupportedType(constructor, StateConstructor.class);
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
        String newStateId = constructor.isSingleton()
            ? constructor.getName()
            : UUID.randomUUID().toString().replace("-", "");
        Object newState = constructor.construct();
        if (newState == null) {
            throw new Exn("State constructor %s returned null.".formatted(constructor));
        }
        return Pair.of(newStateId, newState);
    }

    private StateConstructor<?> findConstructor(String stateId) {
        StateConstructor<?> constructor = nameToConstructor.get(stateId);
        if (constructor == null) {
            throw new Exn("Cannot find a state constructor for '%s'.".formatted(stateId));
        }
        return constructor;
    }

    private <T> T findTypeSupporter(List<Pair<Class<?>, T>> typeSupporters, Class<?> type, String elemType) {
        for (Pair<Class<?>, T> typeSupporter : typeSupporters) {
            if (type.isAssignableFrom(typeSupporter.getLeft())) {
                return typeSupporter.getRight();
            }
        }
        throw new Exn("Cannot find a %s for %s.".formatted(elemType, type));
    }

    private Class<?> getSupportedType(Object obj, Class<?> interf) {
        for (Type gInterf : obj.getClass().getGenericInterfaces()) {
            if (gInterf instanceof ParameterizedType pt && interf.isAssignableFrom((Class<?>) pt.getRawType())) {
                Type firstType = pt.getActualTypeArguments()[0];
                if (firstType instanceof Class<?> cl) {
                    return cl;
                }
            }
        }
        throw new Exn("Cannot find supported type on %s object (as %s).".formatted(obj, interf));
    }

    private <T> List<Pair<Class<?>, T>> determineSupportedTypes(List<?> objects, Class<?> interf) {
        ArrayList<Pair<Class<?>, T>> res = new ArrayList<>();
        for (Object obj : objects) {
            res.add(Pair.of(getSupportedType(obj, interf), (T) obj));
        }
        return res;
    }
}
