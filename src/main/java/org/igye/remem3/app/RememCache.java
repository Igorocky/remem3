package org.igye.remem3.app;

public interface RememCache {
    String getLastUsedDirWithCards();

    void setLastUsedDirWithCards(String path);

    String getLastCreatedCardType();

    void setLastCreatedCardType(String cardType);
}
