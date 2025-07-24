package org.igye.remem3.app;

import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.dto.BucketDelaysDto;

import java.io.File;
import java.util.List;

public interface Settings {
    String getCacheFile();

    List<String> getLanguages();

    List<String> getDirectoriesWithCards();

    String getCardEditor();

    List<BucketDelaysDto> getBucketDelays();

    List<Pair<String, File>> getExercises();
}
