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
        String gapSecondLang = cache.getStr(PAR_GAP_SECOND_LANG, settings.getLanguages().getFirst());
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
                                    .notes(format("%s\n\n%s:%s", gap.getNotes(), origFileName, gapAns))
                                    .build()
                            );
                        }
                    )
            )
            .filter(p -> !existingTranslateCards.contains(p.getLeft()))
            .toList();
        return State.builder()
            .errors(errors)
            .dirSelector(dirSelector)
            .gapSecondLang(gapSecondLang)
            .newTranslateCards(newTranslateCards)
            .build();
    }

    @Override
    public Optional<Supplier<State>> decodeAction(RequestParams params, State state) {
        return Optional.empty();
    }

    @Override
    public State updateState(State state, Supplier<State> action) {
        return action.get();
    }

    @Override
    public void saveState(State state) {
    }

    @Override
    public String renderState(State st) {
        return simplePageWithTitle("Convert FillGaps to Translate",
            h4(text("Convert FillGaps to Translate")),
            rndErrors(st.getErrors()),
            form(
                rndDirSelector(st.getDirSelector()),
                !st.getErrors().isEmpty() ? null : frag(
                    rndGapSecondLanguage(settings, st.getGapSecondLang())
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
            select(ConvertFillGapsToTranslateController.PAR_GAP_SECOND_LANG, selectedLang,
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
}
