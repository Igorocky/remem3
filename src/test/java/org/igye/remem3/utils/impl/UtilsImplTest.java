package org.igye.remem3.utils.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

class UtilsImplTest {
    private final UtilsImpl utils = new UtilsImpl(new ObjectMapper());

    @Test
    void parseDurations() {
        Assertions.assertEquals(List.of(Duration.ofSeconds(29)), utils.parseDurations("29s"));
        Assertions.assertEquals(List.of(Duration.ofSeconds(60)), utils.parseDurations("1m"));
        Assertions.assertEquals(List.of(Duration.ofSeconds(16 * 60)), utils.parseDurations("16m"));
        Assertions.assertEquals(List.of(Duration.ofSeconds(60 * 60)), utils.parseDurations("1h"));
        Assertions.assertEquals(List.of(Duration.ofSeconds(32 * 60 * 60)), utils.parseDurations("32h"));
        Assertions.assertEquals(List.of(Duration.ofSeconds(60 * 60 * 24)), utils.parseDurations("1d"));
        Assertions.assertEquals(List.of(Duration.ofSeconds(7 * 60 * 60 * 24)), utils.parseDurations("7d"));
        Assertions.assertEquals(
            List.of(
                Duration.ofSeconds(5 * 60 * 60 * 24),
                Duration.ofSeconds(60 * 60 * 3),
                Duration.ofSeconds(60 * 18),
                Duration.ofSeconds(3)
            ),
            utils.parseDurations("5d 3h 18m 3s")
        );
    }

    @Test
    void durationsToStr() {
        Assertions.assertEquals("29s", utils.durationsToStr(List.of(Duration.ofSeconds(29))));
        Assertions.assertEquals("1m", utils.durationsToStr(List.of(Duration.ofSeconds(60))));
        Assertions.assertEquals("16m", utils.durationsToStr(List.of(Duration.ofSeconds(16 * 60))));
        Assertions.assertEquals("1h", utils.durationsToStr(List.of(Duration.ofSeconds(60 * 60))));
        Assertions.assertEquals("1d 8h", utils.durationsToStr(List.of(Duration.ofSeconds(32 * 60 * 60))));
        Assertions.assertEquals("1d", utils.durationsToStr(List.of(Duration.ofSeconds(60 * 60 * 24))));
        Assertions.assertEquals("7d", utils.durationsToStr(List.of(Duration.ofSeconds(7 * 60 * 60 * 24))));
        Assertions.assertEquals(
            "5d 3h 18m 3s",
            utils.durationsToStr(
                List.of(
                    Duration.ofSeconds(5 * 60 * 60 * 24),
                    Duration.ofSeconds(60 * 60 * 3),
                    Duration.ofSeconds(60 * 18),
                    Duration.ofSeconds(3)
                )
            )
        );
    }
}