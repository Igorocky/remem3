package org.igye.remem3.app.impl;

import lombok.Builder;
import lombok.Getter;
import org.igye.remem3.app.App;
import org.igye.remem3.app.RememSettings;

import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class RememSettingsImpl implements RememSettings {
    @Builder.Default
    private List<String> languages = Collections.emptyList();
    @Builder.Default
    private List<String> directoriesWithCards = Collections.emptyList();
    @Builder.Default
    private String cacheFile = "";

    public static RememSettings load(App app) {
        return RememSettingsImpl.builder()
            .languages(Collections.unmodifiableList(app.getPropList("languages", List.of())))
            .languages(Collections.unmodifiableList(app.getPropList("directories_with_cards", List.of())))
            .cacheFile(app.getPropStr("cache_file"))
            .build();
    }
}
