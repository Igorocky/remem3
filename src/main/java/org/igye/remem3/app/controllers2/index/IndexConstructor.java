package org.igye.remem3.app.controllers2.index;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.igye.remem3.app.state.StateConstructor;

import java.util.List;

@RequiredArgsConstructor
public class IndexConstructor implements StateConstructor<IndexState> {
    private final List<StateConstructor> stateConstructors;

    @Override
    public String getName() {
        return "index";
    }

    @Override
    public IndexState construct() {
        return new IndexState(
            stateConstructors.stream()
                .map(StateConstructor::getName)
                .filter(StringUtils::isNotBlank)
                .sorted()
                .toList()
        );
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
