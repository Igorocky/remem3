package org.igye.remem3.app.controllers.cardexplorer;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.components.DirSelectorCmp;
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
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CardExplorerRenderer extends HtmlBuilder implements StateRenderer<CardExplorerState> {
    public static final String PAR_DIR = "CardExplorer_PAR_DIR";
    public static final String PAR_PRIORITY = "CardExplorer_PAR_PRIORITY";
    public static final String PAR_SORT_ASC = "CardExplorer_PAR_SORT_ASC";
    public static final String PAR_RECURSIVE = "CardExplorer_PAR_RECURSIVE";
    public static final String PAR_FILTER = "CardExplorer_PAR_FILTER";
    public static final String PAR_CARD_PATH = "CardExplorer_PAR_CARD_PATH";
    public static final String ACT_OPEN_CARD_IN_EDITOR = "CardExplorer_ACT_OPEN_CARD_IN_EDITOR";
    public static final String ACT_REFRESH_CARD = "CardExplorer_ACT_REFRESH_CARD";
    public static final String ACT_REFRESH_PAGE = "CardExplorer_ACT_REFRESH_PAGE";
    public static final String ACT_SET_PRIORITY = "CardExplorer_ACT_SET_PRIORITY";
    public static final String PAR_EXACT_MATCH_SIDE = "CardExplorer_PAR_EXACT_MATCH_SIDE";
    public static final String ACT_SET_EXACT_MATCH = "CardExplorer_ACT_SET_EXACT_MATCH";
    public static final String ACT_EDIT_PROP = "CardExplorer_ACT_EDIT_PROP";
    public static final String ACT_SAVE_PROP = "CardExplorer_ACT_SAVE_PROP";
    public static final String ACT_CANCEL_EDIT_PROP = "CardExplorer_ACT_CANCEL_EDIT_PROP";
    public static final String PAR_EDITED_CARD_PATH = "CardExplorer_PAR_EDITED_CARD_PATH";
    public static final String PAR_EDITED_PROP = "CardExplorer_PAR_EDITED_PROP";
    public static final String PAR_EDITED_TEXT = "CardExplorer_PAR_EDITED_TEXT";
    public static final String ACT_DELETE_CARD = "CardExplorer_ACT_DELETE_CARD";
    public static final String ACT_DELETE_CARD_CONFIRMED = "CardExplorer_ACT_DELETE_CARD_CONFIRMED";
    public static final String ACT_DELETE_CARD_CANCELED = "CardExplorer_ACT_DELETE_CARD_CANCELED";

    @Override
    public HtmlElem render(CardExplorerState st) {
        if (st.getDeleteCard().isPresent()) {
            return rndConfirmCardDelete(st.getDeleteCard().get());
        } else {
            return simplePageWithTitle("Card explorer",
                rndScrollJs(st),
                h4(text("Card explorer")),
                form(
                    rndDirSelector("Directory", st.getDir()),
                    table(List.of(List.of(
                        inpSubmit(ACT_REFRESH_PAGE, "Reload"),
                        text("%s cards".formatted(st.getCards().size())),
                        rndSortSelector(st),
                        rndPrioritySelector(st),
                        rndFilterInput(st),
                        rndRecursiveCheckbox(st)
                    ))),
                    rndCards(st)
                )
            );
        }
    }

    private HtmlElem rndConfirmCardDelete(Card card) {
        File file = card.getFile().get();
        return form(
            h4(text("Delete this card?")),
            span("color:lightgrey;", text(file.getAbsolutePath())),
            rndCard(card, false, Optional.empty()),
            div(
                inpSubmit(keyValueParam(ACT_DELETE_CARD_CONFIRMED, file.getAbsolutePath()), "Delete"),
                inpSubmit(keyValueParam(ACT_DELETE_CARD_CANCELED, file.getAbsolutePath()), "Cancel")
            )
        );
    }

    private HtmlElem rndPrioritySelector(CardExplorerState st) {
        return st.getPriorities().render();
    }

    public String getId(File file) {
        return RegExUtils.replacePattern(
            file.getAbsolutePath(),
            "[^0-9a-zA-Z]+",
            ""
        );
    }

    private HtmlTag rndFilterInput(CardExplorerState st) {
        HtmlTag inpText = inpText(PAR_FILTER, st.getFilter(), ACT_REFRESH_PAGE);
        if (st.getScrollToId().isEmpty()) {
            inpText = inpText.autofocus();
        }
        return inpText;
    }

    private HtmlElem rndScrollJs(CardExplorerState st) {
        return st.getScrollToId()
            .map(id ->
                h("script", rawText(
                    """
                        document.addEventListener("DOMContentLoaded", () => {
                          document.getElementById("%s")?.scrollIntoView();
                        });
                        """.formatted(id)
                ))
            )
            .orElse(null);
    }

    private HtmlElem rndRecursiveCheckbox(CardExplorerState st) {
        return frag(
            text("Recursive"),
            inpCheckbox(PAR_RECURSIVE, PAR_RECURSIVE, st.isRecursive()).submitOnChange()
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


    private HtmlElem rndCards(CardExplorerState st) {
        return table(
            st.getCards().stream()
                .map(card -> List.of(frag(
                    rndCardFile(card, st.isRecursive()),
                    rndCard(card, true, st.getPropEdit())
                )))
                .toList()
        ).attr("class", "table-single-border list-of-cards");
    }

    private HtmlElem rndCardFile(Card card, boolean renderFullPath) {
        if (card.getFile().isEmpty()) {
            return null;
        }
        File file = card.getFile().get();
        String fileAbsPath = file.getAbsolutePath();
        return frag(
            inpSubmit(keyValueParam(ACT_REFRESH_CARD, fileAbsPath), "Reload").id(getId(file)),
            inpSubmit(keyValueParam(ACT_OPEN_CARD_IN_EDITOR, fileAbsPath), "Edit"),
            inpSubmit(keyValueParam(ACT_DELETE_CARD, fileAbsPath), "Delete"),
            rndPriorities(card),
            span("color:lightgrey;", text("%s Created at %s Order %s".formatted(
                renderFullPath ? fileAbsPath : file.getName(),
                card.getCreatedAt().map(Objects::toString).orElse("?"),
                card.getOrder()
            )))
        );
    }

    private HtmlElem rndPriorities(Card card) {
        return span(
            rndPriority(1, card),
            rndPriority(2, card),
            rndPriority(3, card)
        );
    }

    private HtmlElem rndPriority(int priority, Card card) {
        StringBuilder style = new StringBuilder();
        if (priority == card.getPriority()) {
            style.append("font-weight:bold;");
            if (priority == 1) {
                style.append("color:green;");
            }
            if (priority == 2) {
                style.append("color:orange;");
            }
            if (priority == 3) {
                style.append("color:red;");
            }
        } else {
            style.append("color:lightgrey;cursor:pointer;");
        }
        HtmlTag spanElem = span(style.toString(), text("P" + priority));
        if (priority != card.getPriority()) {
            return a("#", spanElem)
                .attr("style", "text-decoration:none;")
                .attr("onclick", "setNewPriorityForCard(event, '%s', '%s')".formatted(
                    card.getFile().map(File::getAbsolutePath)
                        .orElseThrow(() -> new Exn("cannot determine file path for a card.")),
                    priority
                ));
        }
        return spanElem;
    }


    private HtmlElem rndCard(Card card, boolean editable, Optional<CardPropEdit> propEdit) {
        return switch (card) {
            case Card.FillGaps c -> rndFillGapsCard(c, editable, propEdit);
            case Card.Translate c -> rndTranslateCard(c, editable, propEdit);
        };
    }

    private HtmlElem rndTranslateCard(Card.Translate card, boolean editable, Optional<CardPropEdit> propEdit) {
        List<List<HtmlElem>> elems = new ArrayList<>();
        elems.add(
            rndCardSection(
                rndLangAndExactMatch(
                    card, 1, rndPropName(card, EditableProp.TRANSLATE_TEXT1, card.getLang1(), editable, propEdit),
                    card.isExactMatch1(), editable
                ),
                rndPropValue(card, EditableProp.TRANSLATE_TEXT1, pre(text(card.getText1())), editable, propEdit)
            )
        );
        if (StringUtils.isNotBlank(card.getExample1())) {
            elems.add(rndEditableCardSection(
                card, EditableProp.TRANSLATE_EXAMPLE1, "Example", pre(text(card.getExample1())), editable, propEdit
            ));
        }
        elems.add(
            rndCardSection(
                rndLangAndExactMatch(
                    card, 2, rndPropName(card, EditableProp.TRANSLATE_TEXT2, card.getLang2(), editable, propEdit),
                    card.isExactMatch2(), editable
                ),
                rndPropValue(card, EditableProp.TRANSLATE_TEXT2, pre(text(card.getText2())), editable, propEdit)
            )
        );
        if (StringUtils.isNotBlank(card.getExample2())) {
            elems.add(rndEditableCardSection(
                card, EditableProp.TRANSLATE_EXAMPLE2, "Example", pre(text(card.getExample2())), editable, propEdit
            ));
        }
        if (StringUtils.isNotBlank(card.getNotes())) {
            elems.add(rndEditableCardSection(
                card, EditableProp.TRANSLATE_NOTES, "Notes", pre(text(card.getNotes())), editable, propEdit
            ));
        }
        elems.add(rndAttrsIfPresent(card));
        return div(
            table(elems)
                .attr("class", "table-no-border vertical-align-top first-col-text-align-right")
        );
    }

    private HtmlElem rndFillGapsCard(Card.FillGaps card, boolean editable, Optional<CardPropEdit> propEdit) {
        List<List<HtmlElem>> elems = new ArrayList<>();
        if (StringUtils.isNotBlank(card.getDescr())) {
            elems.add(rndCardSection("Description", pre(text(card.getDescr()))));
        }
        elems.add(rndEditableCardSection(
            card, EditableProp.FILL_GAPS_TEXT, card.getLang(), rndText(card.getText()), editable, propEdit
        ));
        if (StringUtils.isNotBlank(card.getNotes())) {
            elems.add(rndEditableCardSection(
                card, EditableProp.FILL_GAPS_NOTES, "Notes", pre(text(card.getNotes())), editable, propEdit
            ));
        }
        elems.add(rndAttrsIfPresent(card));
        return div(
            table(elems)
                .attr("class", "table-no-border vertical-align-top first-col-text-align-right")
        );
    }

    private HtmlElem rndLangAndExactMatch(
        Card.Translate card, int side, HtmlElem lang, boolean exactMatch, boolean editable
    ) {
        String sign = exactMatch ? "=" : "~";
        if (!editable || card.getFile().isEmpty()) {
            return frag(lang, text(" " + sign));
        }
        return frag(
            lang,
            text(" "),
            a("#", span("cursor:pointer;", text(sign)))
                .attr("style", "text-decoration:none;color:inherit;")
                .attr("onclick", "setExactMatchForCard(event, '%s', '%s', '%s')".formatted(
                    card.getFile().get().getAbsolutePath(), side, !exactMatch
                ))
        );
    }

    private boolean isBeingEdited(Card card, EditableProp prop, boolean editable, Optional<CardPropEdit> propEdit) {
        return editable && propEdit.isPresent() && propEdit.get().getProp() == prop
            && card.getFile().map(file -> file.getAbsolutePath().equals(propEdit.get().getCardPath())).orElse(false);
    }

    private HtmlElem rndPropName(
        Card card, EditableProp prop, String name, boolean editable, Optional<CardPropEdit> propEdit
    ) {
        if (!editable || card.getFile().isEmpty() || isBeingEdited(card, prop, editable, propEdit)) {
            return text(name);
        }
        return span("cursor:pointer;", text(name))
            .attr("onclick", "editCardProp(event, '%s', '%s')".formatted(
                card.getFile().get().getAbsolutePath(), prop.name()
            ));
    }

    private HtmlElem rndPropValue(
        Card card, EditableProp prop, HtmlElem content, boolean editable, Optional<CardPropEdit> propEdit
    ) {
        if (!isBeingEdited(card, prop, editable, propEdit)) {
            return content;
        }
        CardPropEdit edit = propEdit.get();
        return frag(
            rndErrors(edit.getErrors()),
            inpHidden(PAR_EDITED_CARD_PATH, edit.getCardPath()),
            inpHidden(PAR_EDITED_PROP, edit.getProp().name()),
            div(textarea(PAR_EDITED_TEXT, edit.getText(), 100, 5).autofocus()),
            div(
                inpSubmit(ACT_SAVE_PROP, "save"),
                inpSubmit(ACT_CANCEL_EDIT_PROP, "cancel")
            )
        );
    }

    private List<HtmlElem> rndEditableCardSection(
        Card card, EditableProp prop, String name, HtmlElem content, boolean editable, Optional<CardPropEdit> propEdit
    ) {
        return rndCardSection(
            rndPropName(card, prop, name, editable, propEdit),
            rndPropValue(card, prop, content, editable, propEdit)
        );
    }

    private List<HtmlElem> rndCardSection(String name, HtmlElem content) {
        return rndCardSection(text(name), content);
    }

    private List<HtmlElem> rndCardSection(HtmlElem name, HtmlElem content) {
        return List.of(
            div("font-weight:bold;", name),
            div("margin-left:5px;", content)
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

    private List<HtmlElem> rndAttrsIfPresent(Card card) {
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
            ).attr("class", "table-no-border vertical-align-top first-col-text-align-right")
                .attr("style", "font-size:70%;")
        );
    }
}
