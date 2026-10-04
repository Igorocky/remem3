package org.igye.remem3.app.controllers.cardimporter;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;

@AllArgsConstructor
@Getter
public class ImportResult {
    private final long numOfCreatedCards;
    private final File dir;
}
