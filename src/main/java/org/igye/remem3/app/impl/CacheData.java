package org.igye.remem3.app.impl;

import lombok.Data;

@Data
public class CacheData {
    private String lastUsedDirWithCards = "";
    private String lastCreatedCardType = "";
}
