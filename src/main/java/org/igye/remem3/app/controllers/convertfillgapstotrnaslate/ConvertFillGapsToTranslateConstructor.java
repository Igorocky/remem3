package org.igye.remem3.app.controllers.convertfillgapstotrnaslate;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;
import org.igye.remem3.app.controllers.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.fillgaps.TextPart;
import org.igye.remem3.app.state.StateConstructor;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.web.RequestParams;
import org.igye.remem3.web.impl.RequestParamsImpl;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.igye.remem3.app.controllers.convertfillgapstotrnaslate.ConvertFillGapsToTranslateRenderer.ATTR_AUTO_GENERATED_FROM;
import static org.igye.remem3.app.controllers.convertfillgapstotrnaslate.ConvertFillGapsToTranslateRenderer.EXISTING_CARD_KEY_PAT;
import static org.igye.remem3.app.controllers.convertfillgapstotrnaslate.ConvertFillGapsToTranslateRenderer.PAR_DIR_TO_CONVERT_TASKS_IN;
import static org.igye.remem3.app.controllers.convertfillgapstotrnaslate.ConvertFillGapsToTranslateRenderer.PAR_GAP_SECOND_LANG;
import static org.igye.remem3.app.controllers.convertfillgapstotrnaslate.ConvertFillGapsToTranslateRenderer.PAR_SELECTED_CARD;

@RequiredArgsConstructor
@Order(3)
public class ConvertFillGapsToTranslateConstructor implements StateConstructor<State> {

    private final Settings settings;
    private final Cache cache;
    private final CardUtils cardUtils;

    @Override
    public String getName() {
        return "convert_fill_gaps_to_translate";
    }

    @Override
    public String getDisplayName() {
        return "Convert FillGaps cards to Translate cards";
    }

    @Override
    public State construct() {
        return readStateFromParams(RequestParamsImpl.empty());
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public State readStateFromParams(RequestParams params) {
        DirSelectorCmp dirSelector = new DirSelectorCmpImpl(
            settings, cache, PAR_DIR_TO_CONVERT_TASKS_IN, false, params
        );
        String defaultLang = settings.getLanguages().getFirst();
        String gapSecondLangFromParams = params.getParam(
            PAR_GAP_SECOND_LANG,
            cache.getStr(PAR_GAP_SECOND_LANG, defaultLang)
        );
        String gapSecondLang = settings.getLanguages().stream()
            .filter(lang -> lang.equals(gapSecondLangFromParams))
            .findFirst()
            .orElse(defaultLang);
        Set<NewCardKey> selectedCardKeys = Arrays.stream(params.getParams(PAR_SELECTED_CARD))
            .map(this::newCardKeyFromString)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
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
            .map(this::makeKeyForExistingCard)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
        List<Pair<NewCardKey, Card.Translate>> newTranslateCards = fillGapsCards.stream()
            .flatMap(card ->
                card.getText().stream()
                    .filter(TextPart.Gap.class::isInstance)
                    .map(TextPart.Gap.class::cast)
                    .filter(gap -> StringUtils.isNotBlank(gap.getHint()))
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
                                    .example1(
                                        card.getText().stream()
                                            .map(tp -> {
                                                if (tp instanceof TextPart.Text) {
                                                    return ((TextPart.Text) tp).getText();
                                                } else {
                                                    return ((TextPart.Gap) tp).getAnswer();
                                                }
                                            })
                                            .collect(Collectors.joining(" "))
                                    )
                                    .lang2(gapSecondLang)
                                    .text2(gap.getHint())
                                    .exactMatch2(false)
                                    .notes(gap.getNotes())
                                    .attrs(Map.of(
                                        ATTR_AUTO_GENERATED_FROM,
                                        format("%s:%s", origFileName, gapAns)
                                    ))
                                    .build()
                            );
                        }
                    )
            )
            .filter(p -> !existingTranslateCards.contains(p.getLeft()))
            .sorted(Comparator.comparing(p -> p.getRight().getText1().toLowerCase()))
            .toList();
        return State.builder()
            .errors(errors)
            .dir(dirSelector)
            .gapSecondLang(gapSecondLang)
            .newTranslateCards(newTranslateCards)
            .selectedCardKeys(selectedCardKeys)
            .build();
    }

    public Optional<NewCardKey> makeKeyForExistingCard(Card.Translate card) {
        if (!card.getAttrs().containsKey(ATTR_AUTO_GENERATED_FROM)) {
            return Optional.empty();
        }
        String src = card.getAttrs().get(ATTR_AUTO_GENERATED_FROM);
        Matcher matcher = EXISTING_CARD_KEY_PAT.matcher(src);
        if (!matcher.matches()) {
            throw new Exn(format("Cannot parse %s attribute value: '%s'.", ATTR_AUTO_GENERATED_FROM, src));
        }
        return Optional.of(
            NewCardKey.builder()
                .origFileUniqueName(matcher.group(1).trim())
                .gapAns(matcher.group(2).trim())
                .build()
        );
    }

    public Optional<NewCardKey> newCardKeyFromString(String str) {
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
