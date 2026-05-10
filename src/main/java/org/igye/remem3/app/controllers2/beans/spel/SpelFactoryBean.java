package org.igye.remem3.app.controllers2.beans.spel;

import org.springframework.beans.factory.FactoryBean;

public class SpelFactoryBean implements FactoryBean<Object> {
    private final Object object;

    public SpelFactoryBean(Object object) {
        this.object = object;
    }

    @Override
    public Object getObject() {
        return object;
    }

    @Override
    public Class<?> getObjectType() {
        return null;
    }
}
