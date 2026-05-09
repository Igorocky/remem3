package org.igye.remem3.app.controllers2.beans.spel;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.FactoryBean;

@RequiredArgsConstructor
public class SpelFactoryBean implements FactoryBean<Object> {
    private final Object object;

    @Override
    public Object getObject() {
        return object;
    }

    @Override
    public Class<?> getObjectType() {
        return null;
    }
}
