package org.igye.remem3.app.controllers2.beans;

public interface SpelEvaluator {
    <T> T eval(Object rootObj, String expr, Class<T> type);

    default Object eval(Object rootObj, String expr) {
        return eval(rootObj, expr, Object.class);
    }
}
