package org.igye.remem3.app.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.igye.remem3.app.Cards;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.fillgaps.CardFillGaps;
import org.igye.remem3.app.dto.fillgaps.Gap;
import org.igye.remem3.app.dto.fillgaps.Text;
import org.igye.remem3.app.dto.fillgaps.TextPart;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class CardsImpl implements Cards {
    private static final Pattern GAP_PATTERN = Pattern.compile("\\[\\[([^\\[\\]]+)\\]\\]");

    private final Utils utils;

    @Override
    public Card load(File file) {
        if (file.getName().endsWith(".fg.card")) {
            return loadFillGapsCard(file);
        }
        throw new Exn("Unsupported type of card " + file.getAbsolutePath());
    }

    private Card loadFillGapsCard(File file) {
        Map<String, String> props = readProps(file);
        String lang = props.get("###lang");
        if (StringUtils.isBlank(lang)) {
            throw new Exn(String.format("lang is not set for %s", file.getAbsolutePath()));
        }
        return CardFillGaps.builder()
            .lang(lang.trim())
            .text(parseText(props.computeIfAbsent("###text", _ -> "")))
            .notes(props.computeIfAbsent("###notes", _ -> ""))
            .history(Collections.emptyList())
            .build();
    }

    protected List<TextPart> parseText(String str) {
        if (StringUtils.isBlank(str)) {
            throw new Exn("Text cannot be empty");
        }
        Matcher matcher = GAP_PATTERN.matcher(str);
        int lastIdx = 0;
        ArrayList<TextPart> res = new ArrayList<>();
        while (matcher.find()) {
            if (lastIdx < matcher.start()) {
                res.add(Text.builder().text(str.substring(lastIdx, matcher.start()).trim()).build());
            }
            String gapText = matcher.group(1);
            String[] gapParts = gapText.split("\\|");
            if (gapParts.length == 0) {
                throw new Exn(String.format("gapParts.length == 0 for %s", gapText));
            }
            if (gapParts.length > 3) {
                throw new Exn(String.format("gapParts.length > 3 for %s", gapText));
            }
            res.add(
                Gap.builder()
                    .answer(getElemOrEmptyStr(gapParts, 0))
                    .hint(getElemOrEmptyStr(gapParts, 1))
                    .notes(getElemOrEmptyStr(gapParts, 2))
                    .build()
            );
            lastIdx = matcher.end();
        }
        if (lastIdx < str.length()) {
            res.add(Text.builder().text(str.substring(lastIdx).trim()).build());
        }
        return res;
    }

    private String getElemOrEmptyStr(String[] gapParts, int i) {
        if (gapParts.length <= i) {
            return "";
        }
        return gapParts[i].trim();
    }

    private Map<String, String> readProps(File file) {
        HashMap<String, String> res = new HashMap<>();
        List<String> buf = null;
        String key = null;
        for (String line : utils.readLines(file)) {
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
