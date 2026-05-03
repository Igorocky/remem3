package org.igye.remem3.app.controllers2.newcard;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.web.RequestParams;

@RequiredArgsConstructor
public class NewCardUpdater extends HtmlBuilder implements StateUpdater<NewCardState> {

    private final Settings settings;
    private final Cache cache;


    @Override
    public NewCardState update(NewCardState state, RequestParams params) {
        return null;
    }
}
