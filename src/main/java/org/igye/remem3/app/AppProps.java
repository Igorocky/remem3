package org.igye.remem3.app;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("app")
@Getter
@RequiredArgsConstructor
@Builder
public class AppProps {
    private final List<String> languages;
    private final List<String> directoriesWithCards;
    private final String cacheFile;
    private final String cardEditor;
    private final String bucketDelays;
    private final List<String> exercises;
}
