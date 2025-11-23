package org.igye.remem3.tools.subtiles;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

class SubtitlesParserImplTest {
    @Test
    void parseSrtSubtitles() {
        //given
        String subtitles = "1\n" +
            "00:02:08,063 --> 00:02:10,632\n" +
            "There is a price to\n" +
            "be paid for that.\n" +
            "\n" +
            "2\n" +
            "00:02:10,664 --> 00:02:12,233\n" +
            "Of course we'll help you.\n" +
            "\n" +
            "3\n" +
            "00:02:23,177 --> 00:02:25,347\n" +
            "Everything's\n" +
            "changing.";
        SubtitlesParserImpl subtitlesParser = new SubtitlesParserImpl();

        //when
        List<Subtitle> parsed = subtitlesParser.parseSrtSubtitles(Arrays.stream(subtitles.split("\n")).toList());

        //then
        Assertions.assertEquals(
            List.of(
                Subtitle.builder()
                    .idx(1)
                    .start(Duration.ofHours(0).plusMinutes(2).plusSeconds(8).plusMillis(63))
                    .end(Duration.ofHours(0).plusMinutes(2).plusSeconds(10).plusMillis(632))
                    .text(List.of("There is a price to", "be paid for that."))
                    .build(),
                Subtitle.builder()
                    .idx(2)
                    .start(Duration.ofHours(0).plusMinutes(2).plusSeconds(10).plusMillis(664))
                    .end(Duration.ofHours(0).plusMinutes(2).plusSeconds(12).plusMillis(233))
                    .text(List.of("Of course we'll help you."))
                    .build(),
                Subtitle.builder()
                    .idx(3)
                    .start(Duration.ofHours(0).plusMinutes(2).plusSeconds(23).plusMillis(177))
                    .end(Duration.ofHours(0).plusMinutes(2).plusSeconds(25).plusMillis(347))
                    .text(List.of("Everything's", "changing."))
                    .build()
            ),
            parsed
        );
    }

}