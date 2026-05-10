package org.igye.remem3.app.controllers2.beans.converter;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.utils.Utils;
import org.springframework.core.convert.converter.Converter;

import java.time.Duration;

@RequiredArgsConstructor
public class DurationConverter implements Converter<String, Duration> {
    private final Utils utils;

    @Override
    public Duration convert(String durStr) {
        return utils.parseDuration(durStr);
    }
}
