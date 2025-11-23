package org.igye.remem3.tools.subtiles;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class SubtitlesMain {
    private final SubtitlesParser subtitlesParser;

    public static void main(String[] args) {
        new SubtitlesMain(new SubtitlesParserImpl()).run(args[0]);
    }

    private void run(String subtitlesPath) {
        List<Subtitle> subtitles = subtitlesParser.parseSrtSubtitles(new File(subtitlesPath));
        List<String> words = extractWords(subtitles);
        words.forEach(System.out::println);
        System.out.printf("Number of words %s", words.size());
    }

    private List<String> extractWords(List<Subtitle> subtitles) {
        return subtitles.stream()
            .flatMap(s -> s.getText().stream().flatMap(str -> Arrays.stream(str.split("\\s+"))))
            .map(String::toLowerCase)
            .map(w -> w.replace("<i>", "").replace("</i>", ""))
            .map(w -> StringUtils.strip(w, "\"',.?!-$0123456789:"))
            .filter(StringUtils::isNotBlank)
            .distinct()
            .sorted()
            .toList();
    }
}
