package org.igye.remem3.app.controllers.cardexplorer;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.fillgaps.TextPart;
import org.igye.remem3.app.state.StateRenderer;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.utils.Exn;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CardExplorerRenderer extends HtmlBuilder implements StateRenderer<CardExplorerState> {
    public static final String PAR_DIR = "CardExplorer_PAR_DIR";
    public static final String PAR_SORT_ASC = "CardExplorer_PAR_SORT_ASC";
    public static final String ACT_OPEN_CARD_IN_EDITOR = "CardExplorer_ACT_OPEN_CARD_IN_EDITOR";
    public static final String ACT_REFRESH = "CardExplorer_ACT_REFRESH";


    @Override
    public HtmlElem render(CardExplorerState st) {
        return simplePageWithTitle("Card explorer",
            h4(text("Card explorer")),
            form(
                rndDirSelector("Directory", st.getDir()),
                table(List.of(List.of(
                    text("%s cards".formatted(st.getCards().size())),
                    rndSortSelector(st),
                    inpSubmit(ACT_REFRESH, "Reload")
                ))),
                rndCards(st.getCards())
            )
        );
    }

    private HtmlElem rndSortSelector(CardExplorerState st) {
        return select(PAR_SORT_ASC, String.valueOf(st.isSortAsc()),
            Pair.of("true", text("ASC")),
            Pair.of("false", text("DSC"))
        ).submitOnChange();
    }

    private HtmlTag rndDirSelector(String title, DirSelectorCmp dirSelector) {
        return table(List.of(List.of(
            text(title),
            dirSelector.render()
        )));
    }


    private HtmlElem rndCards(List<Card> cards) {
        return table(
            cards.stream()
                .map(card -> List.of(frag(
                    rndCardFile(card),
                    rndCard(card)
                )))
                .toList()
        ).attr("class", "table-single-border list-of-cards");
    }

    private HtmlElem rndCardFile(Card card) {
        if (card.getFile().isEmpty()) {
            return null;
        }
        File file = card.getFile().get();
        return frag(
            inpSubmit(keyValueParam(ACT_OPEN_CARD_IN_EDITOR, file.getName()), "Edit"),
            span("color:lightgrey;", text("%s Created at %s Order %s".formatted(
                file.getName(), card.getCreatedAt().map(Objects::toString).orElse("?"), card.getOrder()
            )))
        );
    }


    private HtmlElem rndCard(Card card) {
        return switch (card) {
            case Card.FillGaps c -> rndFillGapsCard(c);
            case Card.Translate c -> rndTranslateCard(c);
        };
    }

    private HtmlElem rndTranslateCard(Card.Translate card) {
        List<HtmlElem> elems = new ArrayList<>();
        elems.add(
            rndCardSection(card.getLang1() + " " + (card.isExactMatch1() ? "=" : "~"), pre(text(card.getText1())))
        );
        if (StringUtils.isNotBlank(card.getExample1())) {
            elems.add(rndCardSection("Example", pre(text(card.getExample1()))));
        }
        elems.add(
            rndCardSection(card.getLang2() + " " + (card.isExactMatch2() ? "=" : "~"), pre(text(card.getText2())))
        );
        if (StringUtils.isNotBlank(card.getExample2())) {
            elems.add(rndCardSection("Example", pre(text(card.getExample2()))));
        }
        if (StringUtils.isNotBlank(card.getNotes())) {
            elems.add(rndCardSection("Notes", pre(text(card.getNotes()))));
        }
        elems.add(rndAttrsIfPresent(card));
        return div(elems);
    }

    private HtmlElem rndFillGapsCard(Card.FillGaps card) {
        List<HtmlElem> elems = new ArrayList<>();
        if (StringUtils.isNotBlank(card.getDescr())) {
            elems.add(rndCardSection("Description", pre(text(card.getDescr()))));
        }
        elems.add(rndCardSection(card.getLang(), rndText(card.getText())));
        if (StringUtils.isNotBlank(card.getNotes())) {
            elems.add(rndCardSection("Notes", pre(text(card.getNotes()))));
        }
        elems.add(rndAttrsIfPresent(card));
        return div(elems);
    }

    private HtmlElem rndCardSection(String name, HtmlElem content) {
        return frag(
            div("font-weight:bold; margin-top:3px;margin-left:10px", text(name)),
            div("margin-left:10px;", content)
        );
    }

    private HtmlElem rndText(List<TextPart> text) {
        return text(
            text.stream()
                .map(part -> {
                    if (part instanceof TextPart.Text t) {
                        return t.getText();
                    } else if (part instanceof TextPart.Gap gap) {
                        return "[[ %s | %s | %s ]]".formatted(
                            gap.getAnswer(), gap.getHint(), gap.getNotes()
                        );
                    } else {
                        throw new Exn("Unexpected text part type %s.".formatted(part));
                    }
                })
                .collect(Collectors.joining(" "))
        );
    }

    private HtmlElem rndAttrsIfPresent(Card card) {
        if (card.getAttrs().isEmpty()) {
            return null;
        }
        return rndCardSection(
            "Attributes",
            table(
                card.getAttrs().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> List.of(text(entry.getKey() + ":"), text(entry.getValue())))
                    .toList()
            ).attr("class", "table-no-border")
        );
    }

}
