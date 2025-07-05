package org.igye.remem3.app;

public interface Cache {
    String getStr(String key, String defaultValue);

    Long getLong(String key, Long defaultValue);

    void put(String key, Object value);
}
