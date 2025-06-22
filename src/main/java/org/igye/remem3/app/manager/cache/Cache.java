package org.igye.remem3.app.manager.cache;

public interface Cache {
    String get(String key);

    Long getLong(String key);

    void set(String key, String value);

    void set(String key, Long value);

    void remove(String key);

    void reload();
}
