package org.igye.remem3.app.impl;

import org.apache.commons.lang3.StringUtils;
import org.igye.remem3.app.TaskTypeMatcher;
import org.igye.remem3.app.dto.TaskType;
import org.igye.remem3.utils.Exn;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaskTypeMatcherImpl implements TaskTypeMatcher {
    private static Pattern fillGapsPattern = Pattern.compile("^fill_gaps(:([_a-zA-Z0-9]+))?$");
    private boolean fillGaps;
    private Optional<String> fillGapsLang = Optional.empty();

    public TaskTypeMatcherImpl(String typStr) {
        Matcher matcher = fillGapsPattern.matcher(typStr);
        if (matcher.matches()) {
            fillGaps = true;
            String lang = matcher.group(2);
            if (StringUtils.isNotBlank(lang) && !"_".equals(lang)) {
                fillGapsLang = Optional.of(lang);
            }
            return;
        }
        throw new Exn(String.format("Cannot parse task type pattern '%s'", typStr));
    }

    @Override
    public boolean matches(TaskType taskType) {
        return switch (taskType) {
            case TaskType.FillGaps typ -> fillGaps && fillGapsLang.map(lang -> lang.equals(typ.getLang())).orElse(true);
        };
    }
}
