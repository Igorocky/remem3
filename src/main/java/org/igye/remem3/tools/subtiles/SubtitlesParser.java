package org.igye.remem3.tools.subtiles;

import java.io.File;
import java.util.List;

public interface SubtitlesParser {
    List<Subtitle> parseSrtSubtitles(File file);

    List<Subtitle> parseSrtSubtitles(List<String> lines);
}
