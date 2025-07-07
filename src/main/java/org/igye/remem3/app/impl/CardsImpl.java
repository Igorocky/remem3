package org.igye.remem3.app.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.igye.remem3.app.Cards;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.fillgaps.TextPart;
import org.igye.remem3.controllers.newcard.CardDto;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class CardsImpl implements Cards {
    private static final Pattern GAP_PATTERN = Pattern.compile("\\[\\[([^\\[\\]]*)\\]\\]");
    private static final Pattern HIST_PATTERN = Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z)\\s+(\\S+)\\s+(\\d+(\\.\\d+)?)(\\s+(.*))?$"
    );
    private static final String ATTR_NAME_LANG = "###lang";
    private static final String ATTR_NAME_TEXT = "###text";
    private static final String ATTR_NAME_NOTES = "###notes";
    private static final String ATTR_NAME_HIST = "###hist";
    private static final String ATTR_NAME_CREATED_AT = "###created_at";
    private static final String CARD_EXTENSION = ".card";
    public static final String CARD_FILL_GAPS_FILE_EXTENSION = ".fg" + CARD_EXTENSION;

    private final Utils utils;
    private final Settings settings;

    @Override
    public Card loadCard(File file) {
        try {
            if (file.getName().endsWith(CARD_FILL_GAPS_FILE_EXTENSION)) {
                return loadFillGapsCard(file);
            }
            throw new Exn("Unsupported type of card " + file.getAbsolutePath());
        } catch (Exception ex) {
            throw new Exn(
                String.format(
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
                .filter(file -> file.getName().endsWith(".card"))
                .map(this::loadCard)
                .toList();
        }
    }

    @Override
    public void saveCard(File file, Card card) {
        try {
            file.getParentFile().mkdirs();
            switch (card) {
                case Card.FillGaps c -> utils.writeStringToFile(fillGapsCardToString(c), file);
            }
        } catch (Exception ex) {
            throw new Exn(
                String.format(
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
        return switch (card) {
            case Card.FillGaps c -> validateFillGapsCard(c);
        };
    }

    @Override
    public void appendHistRecToFile(File file, HistRec histRec) {
        throw new Exn("Not implemented.");
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
        };
    }

    private List<String> validateFillGapsCard(Card.FillGaps card) {
        ArrayList<String> res = new ArrayList<>();
        String lang = card.getLang();
        if (StringUtils.isBlank(lang)) {
            res.add("Language is not set.");
        } else if (!settings.getLanguages().contains(lang)) {
            res.add(String.format("Language '%s' is not registered.", lang));
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

    protected String fillGapsCardToString(Card.FillGaps card) {
        StringBuilder sb = new StringBuilder();
        sb.append(ATTR_NAME_LANG).append("\n").append(card.getLang());
        sb.append("\n\n").append(ATTR_NAME_TEXT).append("\n");
        appendText(sb, card.getText());
        sb.append("\n\n").append(ATTR_NAME_NOTES).append("\n").append(card.getNotes());
        sb.append("\n\n").append(ATTR_NAME_CREATED_AT).append("\n").append(
            card.getCreatedAt().map(Instant::toString).orElse("")
        );
        sb.append("\n\n").append(ATTR_NAME_HIST);
        appendHistory(sb, card.getHistory());
        return sb.toString();
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
        history.forEach(histRec ->
            sb
                .append("\n")
                .append(histRec.getTime().toString())
                .append(" ").append(histRec.getTaskType())
                .append(" ").append(histRec.getMark())
                .append(" ").append(histRec.getNotes())
        );
    }

    private Card loadFillGapsCard(File file) {
        return parseFillGapsCard(utils.readStringFromFile(file), Optional.of(file));
    }

    protected Card parseFillGapsCard(String str, Optional<File> file) {
        Map<String, String> props = parseProps(str);
        String lang = props.get(ATTR_NAME_LANG);
        if (StringUtils.isBlank(lang)) {
            lang = "";
        }
        return Card.FillGaps.builder()
            .file(file)
            .createdAt(
                Optional.ofNullable(props.get(ATTR_NAME_CREATED_AT))
                    .filter(StringUtils::isNotBlank)
                    .map(Instant::parse)
            )
            .lang(lang.trim())
            .text(parseText(props.computeIfAbsent(ATTR_NAME_TEXT, _ -> "")))
            .notes(props.computeIfAbsent(ATTR_NAME_NOTES, _ -> "").trim())
            .history(parseHistory(props.computeIfAbsent(ATTR_NAME_HIST, _ -> "")))
            .build();
    }

    protected List<HistRec> parseHistory(String str) {
        return Arrays.stream(str.split("[\n\r]+"))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .map(this::parseHistoryRec)
            .toList();
    }

    protected HistRec parseHistoryRec(String str) {
        Matcher matcher = HIST_PATTERN.matcher(str);
        if (!matcher.matches()) {
            throw new Exn(String.format("Cannot parse a history record: %s", str));
        }
        String notes = matcher.group(5);
        return HistRec.builder()
            .time(Instant.parse(matcher.group(1)))
            .taskType(matcher.group(2))
            .mark(Double.parseDouble(matcher.group(3)))
            .notes(StringUtils.isNotBlank(notes) ? notes.trim() : "")
            .build();
    }

    protected List<TextPart> parseText(String str) {
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
                throw new Exn(String.format("gapParts.length > 3 for %s", gapText));
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

    private String getElemOrEmptyStr(String[] gapParts, int i) {
        if (gapParts.length <= i) {
            return "";
        }
        return gapParts[i].trim();
    }

    private Map<String, String> parseProps(String str) {
        HashMap<String, String> res = new HashMap<>();
        List<String> buf = null;
        String key = null;
        List<String> lines = Arrays.stream(str.split("[\\n\\r]+"))
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .toList();
        for (String line : lines) {
            if (line.startsWith("###")) {
                if (key != null) {
                    res.put(key, StringUtils.join(buf, "\n"));
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
            res.put(key, StringUtils.join(buf, "\n"));
        }
        return res;
    }
}
