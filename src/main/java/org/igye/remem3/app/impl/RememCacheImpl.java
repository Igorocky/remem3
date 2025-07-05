package org.igye.remem3.app.impl;

import lombok.extern.slf4j.Slf4j;
import org.igye.remem3.app.RememCache;
import org.igye.remem3.app.RememSettings;
import org.igye.remem3.utils.Utils;

import java.io.File;

@Slf4j
public class RememCacheImpl implements RememCache {
    private final Utils utils;
    private final File fileWithCacheData;
    private final CacheData cacheData;

    public static RememCache load(Utils utils, RememSettings settings) {
        return new RememCacheImpl(utils, new File(settings.getCacheFile()));
    }

    private RememCacheImpl(Utils utils, File fileWithCacheData) {
        this.utils = utils;
        this.fileWithCacheData = fileWithCacheData;
        CacheData cacheData;
        try {
            cacheData = utils.parseJson(utils.readStringFromFile(fileWithCacheData), CacheData.class);
        } catch (Exception ex) {
            log.warn(ex.getMessage(), ex);
            cacheData = new CacheData();
        }
        this.cacheData = cacheData;
    }

    @Override
    public String getLastUsedDirWithCards() {
        return cacheData.getLastUsedDirWithCards();
    }

    @Override
    public void setLastUsedDirWithCards(String path) {
        cacheData.setLastUsedDirWithCards(path);
        saveCacheDataToFile();
    }

    @Override
    public String getLastCreatedCardType() {
        return cacheData.getLastCreatedCardType();
    }

    @Override
    public void setLastCreatedCardType(String cardType) {
        cacheData.setLastCreatedCardType(cardType);
        saveCacheDataToFile();
    }

    private void saveCacheDataToFile() {
        utils.writeStringToFile(utils.objToJson(cacheData), fileWithCacheData);
    }
}
