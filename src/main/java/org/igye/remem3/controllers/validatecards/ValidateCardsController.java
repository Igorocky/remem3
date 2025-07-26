package org.igye.remem3.controllers.validatecards;

import org.apache.commons.collections4.CollectionUtils;
import org.igye.remem3.app.App;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.web.RequestParams;
import org.igye.remem3.web.StatefulWebController;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ValidateCardsController extends HtmlBuilder
    implements StatefulWebController<ValidateCardsState, Supplier<ValidateCardsState>> {

    private static final String PAR_DIR = "PAR_DIR";

    private static final String ACT_VALIDATE = "ACT_VALIDATE";

    private final App app;

    public ValidateCardsController(App app) {
        this.app = app;
    }

    @Override
    public String getPath() {
        return "validate_cards";
    }

    @Override
    public ValidateCardsState loadState(RequestParams params) {
        return ValidateCardsState.builder()
            .dir(params.hasParam(PAR_DIR) ? params.getParam(PAR_DIR) : "")
            .build();
    }

    @Override
    public Optional<Supplier<ValidateCardsState>> decodeAction(RequestParams params, ValidateCardsState state) {
        if (CollectionUtils.isNotEmpty(state.getErrors())) {
            return Optional.empty();
        }
        if (params.hasParam(ACT_VALIDATE)) {
            return Optional.of(() -> actValidateCards(state));
        }
        return Optional.empty();
    }

    @Override
    public ValidateCardsState updateState(ValidateCardsState state, Supplier<ValidateCardsState> action) {
        if (CollectionUtils.isNotEmpty(state.getErrors())) {
            return state;
        }
        return action.get();
    }

    @Override
    public void saveState(ValidateCardsState state) {
    }

    @Override
    public String renderState(ValidateCardsState st) {
        return simplePageWithTitle("Validate cards",
            rndErrors(st.getErrors()),
            h3(text("Validate cards")),
            form(
                div(
                    text("Directory: "),
                    inpText(PAR_DIR, st.getDir(), ACT_VALIDATE, true).attr("size", "100")
                ),
                div(inpSubmit(ACT_VALIDATE, "Validate"))
            )
        ).toString();
    }

    private ValidateCardsState actValidateCards(ValidateCardsState state) {
        throw new Exn("not implemented");
    }

    private HtmlElem rndErrors(List<String> errors) {
        if (CollectionUtils.isEmpty(errors)) {
            return null;
        }
        return div("color:red;",
            h3(text("Error")),
            ul(errors.stream().map(msg -> pre(text(msg))).toList())
        );
    }
}
