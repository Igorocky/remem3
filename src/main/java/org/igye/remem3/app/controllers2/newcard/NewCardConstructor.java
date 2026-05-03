package org.igye.remem3.app.controllers2.newcard;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.state.StateConstructor;
import org.igye.remem3.web.impl.RequestParamsImpl;

@RequiredArgsConstructor
public class NewCardConstructor implements StateConstructor<NewCardState> {
    private final NewCardUtils newCardUtils;

    @Override
    public String getName() {
        return "new_card";
    }

    @Override
    public NewCardState construct() {
        return newCardUtils.readStateFromParams(RequestParamsImpl.empty());
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
