package org.igye.remem3.app.dto;

import org.igye.remem3.app.CardType;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface Card {
    CardType getType();

    Optional<File> getFile();

    Optional<Instant> getCreatedAt();

    List<HistRec> getHistory();
}
