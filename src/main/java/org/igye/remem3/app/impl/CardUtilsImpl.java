package org.igye.remem3.app.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers.newcard.CardDto;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.RepeatStrategyType;
import org.igye.remem3.app.dto.fillgaps.TextPart;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.igye.remem3.app.controllers.convertfillgapstotrnaslate.ConvertFillGapsToTranslateRenderer.ATTR_AUTO_GENERATED_FROM;

@RequiredArgsConstructor
public class CardUtilsImpl implements CardUtils {
    private static final Pattern GAP_PATTERN = Pattern.compile("\\[\\[([^\\[\\]]*)\\]\\]");
    private static final Pattern HIST_PATTERN = Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z)\\s+(\\S+)\\s+(\\S+)\\s+(\\d+(\\.\\d+)?)(\\s+(.*))?$"
    );
    private static final String ATTR_LANG = "###lang";
    private static final String ATTR_LANG_1 = "###lang1";
    private static final String ATTR_LANG_2 = "###lang2";
    private static final String ATTR_EXACT_MATCH_1 = "###exact_match1";
    private static final String ATTR_EXACT_MATCH_2 = "###exact_match2";
    private static final String ATTR_DESCR = "###descr";
    private static final String ATTR_TEXT = "###text";
    private static final String ATTR_TEXT_1 = "###text1";
    private static final String ATTR_TEXT_2 = "###text2";
    private static final String ATTR_EXAMPLE_1 = "###example1";
    private static final String ATTR_EXAMPLE_2 = "###example2";
    private static final String ATTR_NOTES = "###notes";
    private static final String ATTR_HIST = "###hist";
    private static final String ATTR_CREATED_AT = "###created_at";
    private static final String ATTR_PREFIX = "###attr__";
    private static final String CARD_EXTENSION = ".card";
    public static final String CARD_FILL_GAPS_FILE_EXTENSION = ".fg" + CARD_EXTENSION;
    public static final String CARD_TRANSLATE_FILE_EXTENSION = ".tr" + CARD_EXTENSION;
    public static final DateTimeFormatter HIST_TIME_FORMATTER = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    );

    private final Utils utils;
    private final Settings settings;

    @Override
    public Card loadCard(File file) {
        try {
            if (file.getName().endsWith(CARD_FILL_GAPS_FILE_EXTENSION)) {
                return loadFillGapsCard(file);
            } else if (file.getName().endsWith(CARD_TRANSLATE_FILE_EXTENSION)) {
                return loadTranslateCard(file);
            }
            throw new Exn("Unsupported type of card " + file.getAbsolutePath());
        } catch (Exception ex) {
            throw new Exn(
                format(
                    "An exception occurred while reading a card from the file %s: %s",
                    file.getAbsolutePath(),
                    ex.getMessage()
                ),
                ex
            );
        }
    }

    @SneakyThrows
    @Override
    public List<Card> loadAllCards(File dir) {
        try (Stream<Path> stream = Files.walk(dir.toPath())) {
            return stream
                .map(Path::toFile)
                .filter(File::isFile)
                .filter(file -> file.getName().endsWith(CARD_EXTENSION))
                .map(this::loadCard)
                .toList();
        }
    }

    @Override
    public void saveCard(File file, Card card) {
        try {
            file.getParentFile().mkdirs();
            utils.writeStringToFile(
                switch (card) {
                    case Card.FillGaps c -> fillGapsCardToString(c);
                    case Card.Translate c -> translateCardToString(c);
                },
                file
            );
        } catch (Exception ex) {
            throw new Exn(
                format(
                    "An exception occurred while writing a card to the file %s: %s",
                    file.getAbsolutePath(),
                    ex.getMessage()
                ),
                ex
            );
        }
    }

    @Override
    public List<String> validateCard(Card card) {
        List<String> errors = switch (card) {
            case Card.FillGaps c -> validateFillGapsCard(c);
            case Card.Translate c -> validateTranslateCard(c);
        };
        card.getFile().map(File::getParentFile).ifPresent(dir -> getReferencedCards(card).forEach(refCardName -> {
            File refCardFile = new File(dir, refCardName);
            if (!refCardFile.exists()) {
                errors.add("A referenced card %s doesn't exist.".formatted(refCardName));
            }
        }));
        return errors;
    }

    @Override
    public List<String> getReferencedCards(Card card) {
        return switch (card) {
            case Card.FillGaps _ -> List.of();
            case Card.Translate c -> {
                String generatedFrom = c.getAttrs().get(ATTR_AUTO_GENERATED_FROM);
                if (generatedFrom != null) {
                    yield List.of(generatedFrom.substring(0, Math.max(generatedFrom.indexOf(":"), 0)));
                } else {
                    yield List.of();
                }
            }
        };
    }

    @SneakyThrows
    @Override
    public void appendHistRecToFile(File file, HistRec histRec) {
        try (FileWriter wr = new FileWriter(file, true)) {
            wr.append("\n").append(histRecToStr(histRec));
        }
    }

    @Override
    public Card makeCard(CardDto cardDto) {
        return switch (cardDto) {
            case CardDto.FillGaps dto -> Card.FillGaps.builder()
                .createdAt(Optional.of(Instant.now()))
                .lang(dto.getLang())
                .text(parseText(dto.getText()))
                .notes(dto.getNotes())
                .history(List.of())
                .build();
            case CardDto.Translate dto -> Card.Translate.builder()
                .createdAt(Optional.of(Instant.now()))
                .lang1(dto.getLang1())
                .text1(dto.getText1())
                .exactMatch1(dto.isExactMatch1())
                .example1(dto.getExample1())
                .lang2(dto.getLang2())
                .text2(dto.getText2())
                .exactMatch2(dto.isExactMatch2())
                .example2(dto.getExample2())
                .notes(dto.getNotes())
                .history(List.of())
                .build();
        };
    }

    @Override
    public String makeFileNameForCard(Card card) {
        String baseName = UUID.randomUUID().toString().replace("-", "_");
        String extension = switch (card) {
            case Card.FillGaps _ -> CARD_FILL_GAPS_FILE_EXTENSION;
            case Card.Translate _ -> CARD_TRANSLATE_FILE_EXTENSION;
        };
        return baseName + extension;
    }

    @Override
    public List<TextPart> parseText(String str) {
        if (StringUtils.isBlank(str)) {
            return Collections.emptyList();
        }
        Matcher matcher = GAP_PATTERN.matcher(str);
        int lastIdx = 0;
        ArrayList<TextPart> res = new ArrayList<>();
        while (matcher.find()) {
            if (lastIdx < matcher.start()) {
                res.add(TextPart.Text.builder().text(str.substring(lastIdx, matcher.start()).trim()).build());
            }
            String gapText = matcher.group(1);
            String[] gapParts = gapText.split("\\|");
            if (gapParts.length > 3) {
                throw new Exn(format("gapParts.length > 3 for %s", gapText));
            }
            res.add(
                TextPart.Gap.builder()
                    .answer(getElemOrEmptyStr(gapParts, 0))
                    .hint(getElemOrEmptyStr(gapParts, 1))
                    .notes(getElemOrEmptyStr(gapParts, 2))
                    .build()
            );
            lastIdx = matcher.end();
        }
        if (lastIdx < str.length()) {
            res.add(TextPart.Text.builder().text(str.substring(lastIdx).trim()).build());
        }
        return res;
    }

    private List<String> validateFillGapsCard(Card.FillGaps card) {
        ArrayList<String> res = new ArrayList<>();
        String lang = card.getLang();
        if (StringUtils.isBlank(lang)) {
            res.add("Language is not set.");
        } else if (!settings.getLanguages().contains(lang)) {
            res.add(format("Language '%s' is not registered.", lang));
        }
        List<TextPart> text = card.getText();
        if (CollectionUtils.isEmpty(text)) {
            res.add("Text is empty.");
        } else {
            StringBuilder sb = new StringBuilder();
            appendText(sb, text);
            if (StringUtils.isBlank(sb.toString())) {
                res.add("Text is empty.");
            } else {
                long numOfGaps = text.stream()
                    .filter(part -> part instanceof TextPart.Gap)
                    .count();
                if (numOfGaps == 0) {
                    res.add("At least one gap must be defined.");
                } else {
                    res.addAll(
                        text.stream()
                            .filter(part -> part instanceof TextPart.Gap gap && StringUtils.isBlank((gap).getAnswer()))
                            .map(_ -> "A gap cannot be empty.")
                            .toList()
                    );
                }
            }
        }
        return res;
    }

    private List<String> validateTranslateCard(Card.Translate card) {
        ArrayList<String> res = new ArrayList<>();
        String lang1 = card.getLang1();
        if (StringUtils.isBlank(lang1)) {
            res.add("Language1 is not set.");
        } else if (!settings.getLanguages().contains(lang1)) {
            res.add(format("Language1 '%s' is not registered.", lang1));
        }
        String text1 = card.getText1();
        if (StringUtils.isBlank(text1)) {
            res.add("Text1 is not set.");
        }
        String lang2 = card.getLang2();
        if (StringUtils.isBlank(lang2)) {
            res.add("Language2 is not set.");
        } else if (!settings.getLanguages().contains(lang2)) {
            res.add(format("Language2 '%s' is not registered.", lang2));
        }
        String text2 = card.getText2();
        if (StringUtils.isBlank(text2)) {
            res.add("Text2 is not set.");
        }
        if (lang1.equals(lang2)) {
            res.add("Languages must be different.");
            //otherwise we will have two identical tasks pointing to the same card and this will cause problems
            //in repeat strategies
        }
        return res;
    }

    protected String fillGapsCardToString(Card.FillGaps card) {
        StringBuilder sb = new StringBuilder();
        sb.append(ATTR_LANG).append("\n").append(card.getLang());
        sb.append("\n\n").append(ATTR_DESCR).append("\n").append(card.getDescr());
        sb.append("\n\n").append(ATTR_TEXT).append("\n");
        appendText(sb, card.getText());
        sb.append("\n\n").append(ATTR_NOTES).append("\n").append(card.getNotes());
        saveAttrs(sb, card.getAttrs());
        appendCreatedAtAndHist(sb, card.getCreatedAt(), card.getHistory());
        return sb.toString();
    }

    protected String translateCardToString(Card.Translate card) {
        StringBuilder sb = new StringBuilder();
        sb.append(ATTR_LANG_1).append("\n").append(card.getLang1());
        sb.append("\n\n").append(ATTR_TEXT_1).append("\n").append(card.getText1());
        sb.append("\n\n").append(ATTR_EXACT_MATCH_1).append("\n").append(card.isExactMatch1() ? "y" : "n");
        sb.append("\n\n").append(ATTR_EXAMPLE_1).append("\n").append(card.getExample1());
        sb.append("\n\n").append(ATTR_LANG_2).append("\n").append(card.getLang2());
        sb.append("\n\n").append(ATTR_TEXT_2).append("\n").append(card.getText2());
        sb.append("\n\n").append(ATTR_EXACT_MATCH_2).append("\n").append(card.isExactMatch2() ? "y" : "n");
        sb.append("\n\n").append(ATTR_EXAMPLE_2).append("\n").append(card.getExample2());
        sb.append("\n\n").append(ATTR_NOTES).append("\n").append(card.getNotes());
        saveAttrs(sb, card.getAttrs());
        appendCreatedAtAndHist(sb, card.getCreatedAt(), card.getHistory());
        return sb.toString();
    }

    private void saveAttrs(StringBuilder sb, Map<String, String> attrs) {
        attrs.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> sb.append("\n\n").append(ATTR_PREFIX).append(e.getKey()).append("\n").append(e.getValue()));
    }

    private void appendCreatedAtAndHist(StringBuilder sb, Optional<Instant> createdAt, List<HistRec> hist) {
        sb.append("\n\n").append(ATTR_CREATED_AT).append("\n").append(
            createdAt.map(this::instantToStr).orElse("")
        );
        sb.append("\n\n").append(ATTR_HIST);
        appendHistory(sb, hist);
    }

    private void appendText(StringBuilder sb, List<TextPart> text) {
        if (CollectionUtils.isEmpty(text)) {
            return;
        }
        sb.append(textPartToStr(text.getFirst()));
        for (int i = 1; i < text.size(); i++) {
            sb.append(" ").append(textPartToStr(text.get(i)));
        }
    }

    private String textPartToStr(TextPart textPart) {
        return switch (textPart) {
            case TextPart.Text text -> text.getText();
            case TextPart.Gap gap -> {
                StringBuilder sb = new StringBuilder("[[").append(gap.getAnswer());
                String hint = gap.getHint();
                boolean hintAdded = false;
                if (StringUtils.isNotBlank(hint)) {
                    sb.append("|").append(hint);
                    hintAdded = true;
                }
                String notes = gap.getNotes();
                if (StringUtils.isNotBlank(notes)) {
                    if (!hintAdded) {
                        sb.append("|");
                    }
                    sb.append("|").append(notes);
                }
                sb.append("]]");
                yield sb.toString();
            }
        };
    }

    private void appendHistory(StringBuilder sb, List<HistRec> history) {
        history.forEach(histRec -> sb.append("\n").append(histRecToStr(histRec)));
    }

    private String instantToStr(Instant inst) {
        return HIST_TIME_FORMATTER.format(inst.atOffset(ZoneOffset.UTC));
    }

    protected String histRecToStr(HistRec histRec) {
        StringBuilder sb = new StringBuilder();
        sb.append(instantToStr(histRec.getTime()))
            .append(" ").append(histRec.getStrategy())
            .append(" ").append(histRec.getTaskType())
            .append(" ").append(histRec.getMark())
            .append(" ").append(histRec.getNotes().replace("\n", "\\n").replace("\r", "\\r"));
        return sb.toString();
    }

    private Card loadFillGapsCard(File file) {
        return parseFillGapsCard(utils.readStringFromFile(file), Optional.of(file));
    }

    private Card loadTranslateCard(File file) {
        return parseTranslateCard(utils.readStringFromFile(file), Optional.of(file));
    }

    protected Card parseTranslateCard(String str, Optional<File> file) {
        Map<String, List<String>> props = parseProps(str);
        return Card.Translate.builder()
            .file(file)
            .createdAt(getInstantOpt(props, ATTR_CREATED_AT))
            .lang1(getStr(props, ATTR_LANG_1, "").trim())
            .text1(getStr(props, ATTR_TEXT_1, "").trim())
            .exactMatch1(getBool(props, ATTR_EXACT_MATCH_1, true))
            .example1(getStr(props, ATTR_EXAMPLE_1, "").trim())
            .lang2(getStr(props, ATTR_LANG_2, "").trim())
            .text2(getStr(props, ATTR_TEXT_2, "").trim())
            .exactMatch2(getBool(props, ATTR_EXACT_MATCH_2, true))
            .example2(getStr(props, ATTR_EXAMPLE_2, "").trim())
            .notes(getStr(props, ATTR_NOTES, "").trim())
            .history(parseHistory(props.computeIfAbsent(ATTR_HIST, _ -> List.of())))
            .attrs(extractAttrs(props))
            .build();
    }

    protected Card parseFillGapsCard(String str, Optional<File> file) {
        Map<String, List<String>> props = parseProps(str);
        return Card.FillGaps.builder()
            .file(file)
            .createdAt(getInstantOpt(props, ATTR_CREATED_AT))
            .lang(getStr(props, ATTR_LANG, "").trim())
            .descr(getStr(props, ATTR_DESCR, "").trim())
            .text(parseText(getStr(props, ATTR_TEXT, "").trim()))
            .notes(getStr(props, ATTR_NOTES, "").trim())
            .history(parseHistory(props.computeIfAbsent(ATTR_HIST, _ -> List.of())))
            .attrs(extractAttrs(props))
            .build();
    }

    private Map<String, String> extractAttrs(Map<String, List<String>> props) {
        return props.entrySet().stream()
            .filter(e -> e.getKey().startsWith(ATTR_PREFIX))
            .collect(Collectors.toMap(
                e -> e.getKey().substring(ATTR_PREFIX.length()),
                e -> StringUtils.join(e.getValue(), "\n").trim(),
                (_, _) -> {
                    throw new Exn("There was a duplicated attribute name.");
                },
                HashMap::new
            ));
    }


    private String getStr(Map<String, List<String>> props, String propName, String defaultValue) {
        return StringUtils.join(props.computeIfAbsent(propName, _ -> List.of(defaultValue)), "\n");
    }

    private boolean getBool(Map<String, List<String>> props, String propName, boolean defaultValue) {
        String boolStr = StringUtils.join(
            props.computeIfAbsent(propName, _ -> List.of(defaultValue + "")),
            "\n"
        ).trim();
        if (StringUtils.isEmpty(boolStr)) {
            return defaultValue;
        } else if ("y".equalsIgnoreCase(boolStr) || "true".equalsIgnoreCase(boolStr)) {
            return true;
        } else if ("n".equalsIgnoreCase(boolStr) || "false".equalsIgnoreCase(boolStr)) {
            return false;
        } else {
            throw new Exn(format("Cannot parse a boolean value '%s'.", boolStr));
        }
    }

    private Optional<Instant> getInstantOpt(Map<String, List<String>> props, String propName) {
        return Optional.ofNullable(
                props.containsKey(propName)
                    ? getStr(props, propName, "").trim()
                    : null
            )
            .filter(StringUtils::isNotBlank)
            .map(Instant::parse);
    }

    protected List<HistRec> parseHistory(List<String> hist) {
        return hist.stream()
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .map(this::parseHistoryRec)
            .toList();
    }

    protected HistRec parseHistoryRec(String str) {
        Matcher matcher = HIST_PATTERN.matcher(str);
        if (!matcher.matches()) {
            throw new Exn(format("Cannot parse a history record: %s", str));
        }
        String notes = matcher.group(6);
        return HistRec.builder()
            .time(Instant.parse(matcher.group(1)))
            .strategy(RepeatStrategyType.valueOf(matcher.group(2)))
            .taskType(matcher.group(3))
            .mark(new BigDecimal(matcher.group(4)))
            .notes(StringUtils.isNotBlank(notes) ? notes.trim() : "")
            .build();
    }

    private String getElemOrEmptyStr(String[] gapParts, int i) {
        if (gapParts.length <= i) {
            return "";
        }
        return gapParts[i].trim();
    }

    protected Map<String, List<String>> parseProps(String str) {
        HashMap<String, List<String>> res = new HashMap<>();
        List<String> buf = new ArrayList<>();
        String key = null;
        for (String line : str.split("\\r?\\n")) {
            if (line.startsWith("###")) {
                if (key != null) {
                    putKeyVal(res, key, buf);
                }
                key = line.trim();
                buf = new ArrayList<>();
            } else if (key == null) {
                throw new Exn("key == null");
            } else {
                buf.add(line);
            }
        }
        if (key != null) {
            putKeyVal(res, key, buf);
        }
        return res;
    }

    private void putKeyVal(HashMap<String, List<String>> props, String key, List<String> val) {
        if (!props.containsKey(key)) {
            props.put(key, val);
        } else {
            throw new Exn(format("Duplicated key '%s'.", key));
        }
    }
}
