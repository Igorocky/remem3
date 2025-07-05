package org.igye.remem3.app.dto;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface Card {
    Optional<File> getFile();

    Optional<Instant> getCreatedAt();

    List<HistRec> getHistory();
}
