package org.igye.remem3.app.impl;

import lombok.extern.slf4j.Slf4j;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.utils.Utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class CacheImpl implements Cache {
    private final Utils utils;
    private final File fileWithCacheData;
    private final Map<String, String> cacheData;

    public static Cache load(Utils utils, Settings settings) {
        return new CacheImpl(utils, new File(settings.getCacheFile()));
    }

    private CacheImpl(Utils utils, File fileWithCacheData) {
        this.utils = utils;
        this.fileWithCacheData = fileWithCacheData;
        Map<String, String> cacheData;
        try {
            cacheData = utils.parseJson(utils.readStringFromFile(fileWithCacheData), Map.class);
        } catch (Exception ex) {
            log.warn(ex.getMessage(), ex);
            cacheData = new HashMap<>();
        }
        this.cacheData = cacheData;
    }

    @Override
    public String getStr(String key, String defaultValue) {
        String value = cacheData.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    @Override
    public long getLong(String key, long defaultValue) {
        String value = cacheData.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            log.warn(ex.getMessage(), ex);
            return defaultValue;
        }
    }

    @Override
    public boolean getBool(String key, boolean defaultValue) {
        String value = cacheData.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Boolean.parseBoolean(value);
        } catch (Exception ex) {
            log.warn(ex.getMessage(), ex);
            return defaultValue;
        }
    }

    @Override
    public void put(String key, Object value) {
        cacheData.put(key, String.valueOf(value));
        utils.writeStringToFile(utils.objToJson(cacheData), fileWithCacheData);
    }
}
