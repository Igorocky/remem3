package org.igye.remem3.app.impl;

import org.apache.commons.lang3.StringUtils;
import org.igye.remem3.app.TaskTypeMatcher;
import org.igye.remem3.app.dto.TaskType;
import org.igye.remem3.utils.Exn;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaskTypeMatcherImpl implements TaskTypeMatcher {
    private static final Pattern FILL_GAPS_PATTERN = Pattern.compile("^fill_gaps(:([_a-zA-Z0-9]+))?$");
    private static final Pattern TRANSLATE_PATTERN = Pattern.compile("^translate(:([_a-zA-Z0-9]+)->([_a-zA-Z0-9]+))?$");

    private boolean fillGaps;
    private Optional<String> fillGapsLang = Optional.empty();

    private boolean translate;
    private Optional<String> translateLangFrom = Optional.empty();
    private Optional<String> translateLangTo = Optional.empty();

    public TaskTypeMatcherImpl(String typStr) {
        Matcher matcher = FILL_GAPS_PATTERN.matcher(typStr);
        if (matcher.matches()) {
            fillGaps = true;
            String lang = matcher.group(2);
            if (StringUtils.isNotBlank(lang) && !"_".equals(lang)) {
                fillGapsLang = Optional.of(lang);
            }
            return;
        }
        matcher = TRANSLATE_PATTERN.matcher(typStr);
        if (matcher.matches()) {
            translate = true;
            String langFrom = matcher.group(2);
            if (StringUtils.isNotBlank(langFrom) && !"_".equals(langFrom)) {
                translateLangFrom = Optional.of(langFrom);
            }
            String langTo = matcher.group(3);
            if (StringUtils.isNotBlank(langTo) && !"_".equals(langTo)) {
                translateLangTo = Optional.of(langTo);
            }
            return;
        }
        throw new Exn(String.format("Cannot parse task type pattern '%s'", typStr));
    }

    @Override
    public boolean matches(TaskType taskType) {
        return switch (taskType) {
            case TaskType.FillGaps typ -> fillGaps && fillGapsLang.map(lang -> lang.equals(typ.getLang())).orElse(true);
            case TaskType.Translate typ -> translate
                && translateLangFrom.map(lang -> lang.equals(typ.getLangFrom())).orElse(true)
                && translateLangTo.map(lang -> lang.equals(typ.getLangTo())).orElse(true);
        };
    }
}
