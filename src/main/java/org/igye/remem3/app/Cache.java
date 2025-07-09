package org.igye.remem3.app;

public interface Cache {
    String getStr(String key, String defaultValue);

    long getLong(String key, long defaultValue);

    boolean getBool(String key, boolean defaultValue);

    void put(String key, Object value);
}
