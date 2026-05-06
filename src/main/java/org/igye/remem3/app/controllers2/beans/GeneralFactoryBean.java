package org.igye.remem3.app.controllers2.beans;

public class GeneralFactoryBean {
    public <T> T make(T obj) {
        return obj;
    }
}
