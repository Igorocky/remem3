package org.igye.remem3.app.controllers.convertfillgapstotrnaslate;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;
import org.igye.remem3.app.controllers.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.fillgaps.TextPart;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.web.RequestParams;
import org.igye.remem3.web.StatefulWebController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;

@RequiredArgsConstructor
public class ConvertFillGapsToTranslateController extends HtmlBuilder
    implements StatefulWebController<State, Supplier<State>> {

    private static final String PAR_DIR_TO_CONVERT_TASKS_IN = "PAR_DIR_TO_CONVERT_TASKS_IN";
    private static final String PAR_GAP_SECOND_LANG = "PAR_GAP_SECOND_LANG";
    private static final String PAR_SELECTED_CARD = "PAR_SELECTED_CARD";

    private static final String AUTO_GENERATED_FROM_ = "auto generated from ";
    private static final Pattern EXISTING_CARD_KEY_PAT = Pattern.compile(
        AUTO_GENERATED_FROM_ + "([^:]+):(.*)$",
        Pattern.DOTALL
    );

    private final Settings settings;
    private final Cache cache;
    private final CardUtils cardUtils;

    @Override
    public String getId() {
        return "convert_fill_gaps_to_translate";
    }

    @Override
    public String getTitle() {
        return "Convert FillGaps cards to Translate cards";
    }

    @Override
    public State loadState(RequestParams params) {
        DirSelectorCmp dirSelector = new DirSelectorCmpImpl(settings, cache, params, PAR_DIR_TO_CONVERT_TASKS_IN);
        String gapSecondLang = params.getParam(
            PAR_GAP_SECOND_LANG,
            cache.getStr(PAR_GAP_SECOND_LANG, settings.getLanguages().getFirst())
        );
        List<Card> allCards = cardUtils.loadAllCards(dirSelector.getSelectedDirectory());
        List<Card.FillGaps> fillGapsCards = allCards.stream()
            .filter(c -> c instanceof Card.FillGaps)
            .map(Card.FillGaps.class::cast)
            .toList();
        List<String> nonUniqueNames = fillGapsCards.stream()
            .map(c -> c.getFile().get().getName())
            .collect(Collectors.groupingBy(Function.identity()))
            .entrySet().stream()
            .filter(e -> e.getValue().size() > 1)
            .map(Map.Entry::getKey)
            .sorted()
            .toList();
        List<String> errors = new ArrayList<>();
        if (!nonUniqueNames.isEmpty()) {
            errors.add(format(
                "All FillGaps cards in the selected directory must have unique names, but found non-unique names: %s",
                StringUtils.join(nonUniqueNames, ", ")
            ));
        }
        Set<NewCardKey> existingTranslateCards = allCards.stream()
            .filter(c -> c instanceof Card.Translate)
            .map(Card.Translate.class::cast)
            .map(c -> makeKeyForExistingCard(c.getNotes()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
        List<Pair<NewCardKey, Card.Translate>> newTranslateCards = fillGapsCards.stream()
            .flatMap(card ->
                card.getText().stream()
                    .filter(TextPart.Gap.class::isInstance)
                    .map(TextPart.Gap.class::cast)
                    .map(gap ->
                        {
                            String origFileName = card.getFile().get().getName().trim();
                            String gapAns = gap.getAnswer().trim();
                            return Pair.of(
                                NewCardKey.builder()
                                    .origFileUniqueName(origFileName)
                                    .gapAns(gapAns)
                                    .build(),
                                (Card.Translate) Card.Translate.builder()
                                    .file(card.getFile())
                                    .lang1(card.getLang())
                                    .text1(gap.getAnswer())
                                    .exactMatch1(true)
                                    .lang2(gapSecondLang)
                                    .text2(gap.getHint())
                                    .exactMatch2(false)
                                    .notes(format(
                                        "%s\n\n%s%s:%s",
                                        gap.getNotes(), AUTO_GENERATED_FROM_, origFileName, gapAns
                                    ))
                                    .build()
                            );
                        }
                    )
            )
            .filter(p -> !existingTranslateCards.contains(p.getLeft()))
            .toList();
        Set<NewCardKey> selectedCardKeys = Arrays.stream(params.getParams(PAR_SELECTED_CARD))
            .map(this::newCardKeyFromString)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
        return State.builder()
            .errors(errors)
            .dirSelector(dirSelector)
            .gapSecondLang(gapSecondLang)
            .newTranslateCards(newTranslateCards)
            .selectedCardKeys(selectedCardKeys)
            .build();
    }

    @Override
    public Optional<Supplier<State>> decodeAction(RequestParams params, State st) {
        if (!st.getErrors().isEmpty()) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    @Override
    public State updateState(State st, Supplier<State> action) {
        if (!st.getErrors().isEmpty()) {
            return st;
        }
        return action.get();
    }

    @Override
    public void saveState(State st) {
        cache.put(PAR_DIR_TO_CONVERT_TASKS_IN, st.getDirSelector().getSelectedDirectoryStr());
        cache.put(PAR_GAP_SECOND_LANG, st.getGapSecondLang());
    }

    @Override
    public String renderState(State st) {
        return simplePageWithTitle("Convert FillGaps to Translate",
            h4(text("Convert FillGaps to Translate")),
            rndErrors(st.getErrors()),
            form(
                rndDirSelector(st.getDirSelector()),
                !st.getErrors().isEmpty() ? null : frag(
                    rndGapSecondLanguage(settings, st.getGapSecondLang()),
                    rndCards(st.getNewTranslateCards(), st.getSelectedCardKeys())
                )
            )
        ).toString();
    }

    private HtmlTag rndDirSelector(DirSelectorCmp dirSelector) {
        return table(List.of(List.of(
            text("Directory"),
            frag(dirSelector.render())
        )));
    }

    private HtmlElem rndGapSecondLanguage(Settings settings, String selectedLang) {
        return table(List.of(List.of(
            text("Gap second language"),
            select(ConvertFillGapsToTranslateController.PAR_GAP_SECOND_LANG, true, selectedLang,
                settings.getLanguages().stream()
                    .map(lang -> Pair.of(lang, text(lang)))
                    .toList()
            )
        )));
    }

    protected Optional<NewCardKey> makeKeyForExistingCard(String note) {
        if (note == null) {
            return Optional.empty();
        }
        Matcher matcher = EXISTING_CARD_KEY_PAT.matcher(note);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(
            NewCardKey.builder()
                .origFileUniqueName(matcher.group(1).trim())
                .gapAns(matcher.group(2).trim())
                .build()
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

    private HtmlElem rndCards(List<Pair<NewCardKey, Card.Translate>> cards, Set<NewCardKey> selectedCardKeys) {
        return table(
            cards.stream()
                .map(p -> List.of(
                    inpCheckbox(
                        PAR_SELECTED_CARD, p.getLeft().toString(), selectedCardKeys.contains(p.getLeft())
                    ),
                    rndCard(p.getRight())
                ))
                .toList()
        );
    }

    private HtmlElem rndCard(Card.Translate card) {
        return table(List.of(
            List.of(
                text(format("%s %s", card.getLang1(), card.isExactMatch1() ? "=" : "~")),
                text(card.getText1())
            ),
            List.of(
                text(format("%s %s", card.getLang2(), card.isExactMatch2() ? "=" : "~")),
                text(card.getText2())
            ),
            List.of(
                text(""),
                pre(text(card.getNotes()))
            )
        ));
    }

    protected Optional<NewCardKey> newCardKeyFromString(String str) {
        if (StringUtils.isBlank(str)) {
            return Optional.empty();
        }
        int sepIdx = str.indexOf(":");
        if (sepIdx <= 0 || sepIdx == str.length() - 1) {
            return Optional.empty();
        }
        return Optional.of(
            NewCardKey.builder()
                .origFileUniqueName(str.substring(0, sepIdx).trim())
                .gapAns(str.substring(sepIdx + 1).trim())
                .build()
        );
    }
}
