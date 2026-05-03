package org.igye.remem3.app.controllers2.newcard;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.state.StateConstructor;
import org.igye.remem3.utils.NotImplemented;

@RequiredArgsConstructor
public class NewCardConstructor implements StateConstructor<NewCardState> {
    private final Cache cache;

    @Override
    public String getName() {
        return "create_new_card";
    }

    @Override
    public NewCardState construct() {
        throw new NotImplemented();
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public Class<?> getSupportedType() {
        return NewCardState.class;
    }
}
