package org.igye.remem3.app;

import org.igye.remem3.app.dto.BucketDelaysDto;

import java.util.List;

public interface Settings {
    String getCacheFile();

    List<String> getLanguages();

    List<String> getDirectoriesWithCards();

    String getCardEditor();

    List<BucketDelaysDto> getBucketDelays();
}
