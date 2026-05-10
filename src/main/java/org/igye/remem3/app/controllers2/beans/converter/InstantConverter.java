package org.igye.remem3.app.controllers2.beans.converter;

import org.springframework.core.convert.converter.Converter;

import java.time.Instant;

public class InstantConverter implements Converter<String, Instant> {

    @Override
    public Instant convert(String str) {
        return str == null ? null : Instant.parse(str);
    }
}
