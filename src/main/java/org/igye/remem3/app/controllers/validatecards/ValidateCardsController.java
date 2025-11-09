package org.igye.remem3.app.controllers.validatecards;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.web.RequestParams;
import org.igye.remem3.web.StatefulWebController;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class ValidateCardsController extends HtmlBuilder
    implements StatefulWebController<ValidateCardsState, Supplier<ValidateCardsState>> {

    private static final String PAR_DIR = "PAR_DIR";

    private static final String ACT_VALIDATE = "ACT_VALIDATE";

    private final CardUtils cardUtils;


    @Override
    public String getId() {
        return "validate_cards";
    }

    @Override
    public String getTitle() {
        return "Validate cards";
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
            ),
            rndValidationResults(st)
        ).toString();
    }

    private HtmlElem rndValidationResults(ValidateCardsState st) {
        List<Pair<Card, List<String>>> cardsAndErrors = st.getCardsAndErrors();
        if (CollectionUtils.isEmpty(cardsAndErrors)) {
            return null;
        }
        ArrayList<HtmlElem> content = new ArrayList<>();
        content.add(div(text(String.format("There are %s cards.", cardsAndErrors.size()))));
        long cardsWithErrorsCnt = cardsAndErrors.stream().filter(pair -> !pair.getRight().isEmpty()).count();
        if (cardsWithErrorsCnt == 0) {
            content.add(div(text("All cards are valid.")));
        } else if (cardsWithErrorsCnt == 1) {
            content.add(div(text("1 card has validation errors.")));
        } else {
            content.add(div(text(String.format("%s cards have validation errors.", cardsWithErrorsCnt))));
        }
        content.addAll(
            cardsAndErrors.stream()
                .filter(pair -> !pair.getRight().isEmpty())
                .map(pair -> rndCardErrors(pair.getLeft(), pair.getRight()))
                .toList()
        );
        return div(content);
    }

    private HtmlElem rndCardErrors(Card card, List<String> errors) {
        return div(
            div(text(card.getFile().get().getAbsolutePath())),
            ul(errors.stream().map(this::text).toList())
        ).attr("style", "margin-top:10px;");
    }

    private ValidateCardsState actValidateCards(ValidateCardsState st) {
        String dirStr = st.getDir();
        File dir = new File(dirStr);
        if (!dir.exists()) {
            return st.withErrors(List.of(String.format("The specified directory doesn't exist: %s", dirStr)));
        }
        if (!dir.isDirectory()) {
            return st.withErrors(List.of(String.format("Not a directory: %s", dirStr)));
        }
        List<Card> cards = cardUtils.loadAllCards(dir);
        if (cards.isEmpty()) {
            return st.withErrors(List.of(String.format("The specified directory doesn't contains cards: %s", dirStr)));
        }
        return st.withCardsAndErrors(
            cards.stream()
                .sorted(Comparator.comparing(c -> c.getFile().get().getAbsolutePath()))
                .map(card -> Pair.of(card, cardUtils.validateCard(card)))
                .toList()
        );
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
