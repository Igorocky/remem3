package org.igye.remem3.app.controllers.validatecards;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.state.StateRenderer;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;

import java.util.ArrayList;
import java.util.List;

public class ValidateCardsRenderer extends HtmlBuilder implements StateRenderer<ValidateCardsState> {
    public static final String PAR_DIR = "ValidateCards_PAR_DIR";
    public static final String ACT_VALIDATE = "ValidateCards_ACT_VALIDATE";

    @Override
    public HtmlElem render(ValidateCardsState st) {
        return simplePageWithTitle("Validate cards",
            rndErrors(st.getErrors()),
            h3(text("Validate cards")),
            form(
                table(List.of(List.of(
                    text("Directory"), st.getDir().render()
                ))),
                div(inpSubmit(ACT_VALIDATE, "Validate"))
            ),
            rndValidationResults(st.getCardsAndErrors())
        );
    }

    private HtmlElem rndValidationResults(List<Pair<Card, List<String>>> cardsAndErrors) {
        if (CollectionUtils.isEmpty(cardsAndErrors)) {
            return null;
        }
        ArrayList<HtmlElem> content = new ArrayList<>();
        content.add(div(text("There are %s cards.".formatted(cardsAndErrors.size()))));
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
}
