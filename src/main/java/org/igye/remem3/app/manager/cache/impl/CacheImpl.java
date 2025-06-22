package org.igye.remem3.app.manager.cache.impl;

import org.igye.remem3.app.db.entities.CacheEnt;
import org.igye.remem3.app.manager.cache.Cache;
import org.igye.remem3.utils.sqlite.Database;

import java.util.HashMap;
import java.util.Map;

public class CacheImpl implements Cache {
    private final Database db;
    private final Map<String, CacheEnt> cache = new HashMap<>();

    public CacheImpl(Database db) {
        this.db = db;
        reload();
    }

    @Override
    public String get(String key) {
        CacheEnt ent = cache.get(key);
        if (ent == null) {
            return null;
        }
        return ent.value;
    }

    @Override
    public Long getLong(String key) {
        String value = get(key);
        if (value == null) {
            return null;
        }
        return Long.parseLong(value);
    }

    @Override
    public void set(String key, String value) {
        if (value == null) {
            remove(key);
            return;
        }
        CacheEnt ent = cache.get(key);
        if (ent == null) {
            ent = CacheEnt.builder().key(key).value(value).build();
            db.insert(ent);
        } else {
            String oldValue = ent.value;
            ent.value = value;
            try {
                db.update(ent);
            } catch (Exception e) {
                ent.value = oldValue;
                throw e;
            }
        }
        cache.put(key, ent);
    }

    @Override
    public void set(String key, Long value) {
        set(key, value == null ? null : String.valueOf(value));
    }

    @Override
    public void remove(String key) {
        if (cache.containsKey(key)) {
            db.delete(CacheEnt.class, cache.get(key).id);
            cache.remove(key);
        }
    }

    @Override
    public void reload() {
        cache.clear();
        db.select(CacheEnt.class).forEach(keyVal -> cache.put(keyVal.key, keyVal));
    }
}
