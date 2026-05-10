package org.igye.remem3.app.controllers.index;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.igye.remem3.app.state.StateConstructor;
import org.springframework.core.annotation.Order;

import java.util.List;

@RequiredArgsConstructor
@Order(10_000)
public class IndexConstructor implements StateConstructor<IndexState> {
    private final List<StateConstructor> stateConstructors;

    @Override
    public String getName() {
        return "index";
    }

    @Override
    public String getDisplayName() {
        return "Index";
    }

    @Override
    public IndexState construct() {
        return new IndexState(
            stateConstructors.stream()
                .filter(c -> StringUtils.isNotBlank(c.getName()))
                .toList()
        );
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
