package org.igye.remem3.controllers.newcard;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.utils.web.RequestParams;
import org.igye.remem3.utils.web.impl.RequestParamsImpl;
import org.igye.remem3.web.StatefulWebController;

import java.util.Optional;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class NewCardController extends HtmlBuilder
    implements StatefulWebController<NewCardState, Supplier<NewCardState>> {

    @Override
    public String getPath() {
        return "create_new_card";
    }

    @Override
    public NewCardState loadState(HttpServletRequest req) {
        return null;
    }

    @Override
    public Optional<Supplier<NewCardState>> decodeAction(HttpServletRequest req, NewCardState state) {
        RequestParams params = new RequestParamsImpl(req);
        return Optional.empty();
    }

    @Override
    public NewCardState updateState(NewCardState state, Supplier<NewCardState> action) {
        return action.get();
    }

    @Override
    public void saveState(NewCardState state) {

    }

    @Override
    public String renderState(NewCardState state) {
        return simplePageWithTitle("Add new card",
            form(
                h3(text("Add new card"))
            )
        ).toString();
    }

}
