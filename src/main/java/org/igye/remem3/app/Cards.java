package org.igye.remem3.app;

import org.igye.remem3.app.dto.Card;

import java.io.File;

public interface Cards {
    Card load(File file);
}
