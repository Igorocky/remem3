package org.igye.remem3.app.controllers2.beans;

import lombok.RequiredArgsConstructor;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionService;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class CustomConversionServiceFactoryBean extends ConversionServiceFactoryBean {
    private final Consumer<ConversionService> onConversionServiceCreated;

    @Override
    public ConversionService getObject() {
        ConversionService conversionService = super.getObject();
        onConversionServiceCreated.accept(conversionService);
        return conversionService;
    }
}
