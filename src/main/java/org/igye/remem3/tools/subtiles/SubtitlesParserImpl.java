package org.igye.remem3.tools.subtiles;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.utils.Exn;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class SubtitlesParserImpl implements SubtitlesParser {

    private static final Pattern PAT_DURATION = Pattern.compile("(\\d+):(\\d+):(\\d+),(\\d+)");

    @SneakyThrows
    @Override
    public List<Subtitle> parseSrtSubtitles(File file) {
        return parseSrtSubtitles(FileUtils.readLines(file, StandardCharsets.UTF_8));
    }

    @Override
    public List<Subtitle> parseSrtSubtitles(List<String> lines) {
        ArrayList<Subtitle> res = new ArrayList<>();
        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i++);
            while (StringUtils.isBlank(line)) {
                line = lines.get(i++);
            }
            long idx = Long.parseLong(line);
            Pair<Duration, Duration> period = parsePeriod(lines.get(i++));
            StringBuilder text = new StringBuilder();
            line = lines.get(i++);
            while (StringUtils.isNotBlank(line)) {
                if (!text.isEmpty()) {
                    text.append("\n");
                }
                text.append(line);
                if (i == lines.size()) {
                    break;
                }
                line = lines.get(i++);
            }
            res.add(
                Subtitle.builder()
                    .idx(idx)
                    .start(period.getLeft())
                    .end(period.getRight())
                    .text(text.toString())
                    .build()
            );
        }
        return res;
    }

    private Pair<Duration, Duration> parsePeriod(String str) {
        String[] beginEnd = str.split("-->");
        if (beginEnd.length != 2) {
            throw new Exn(format("Expected format 'begin --> end', but got '%s'", str));
        }
        return Pair.of(parseDuration(beginEnd[0].trim()), parseDuration(beginEnd[1].trim()));
    }

    private Duration parseDuration(String str) {
        Matcher matcher = PAT_DURATION.matcher(str);
        if (!matcher.matches()) {
            throw new Exn(format("Cannot parse duration '%s'.", str));
        }
        return Duration.ofHours(Long.parseLong(matcher.group(1)))
            .plusMinutes(Long.parseLong(matcher.group(2)))
            .plusSeconds(Long.parseLong(matcher.group(3)))
            .plusMillis(Long.parseLong(matcher.group(4)));
    }
}
