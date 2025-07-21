package org.igye.remem3.utils.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

class UtilsImplTest {
    private final UtilsImpl utils = new UtilsImpl(new ObjectMapper());

    @Test
    void parseDuration() {
        Assertions.assertEquals(Duration.ofSeconds(29), utils.parseDuration("29s"));
        Assertions.assertEquals(Duration.ofSeconds(60), utils.parseDuration("1m"));
        Assertions.assertEquals(Duration.ofSeconds(16 * 60), utils.parseDuration("16m"));
        Assertions.assertEquals(Duration.ofSeconds(60 * 60), utils.parseDuration("1h"));
        Assertions.assertEquals(Duration.ofSeconds(32 * 60 * 60), utils.parseDuration("32h"));
        Assertions.assertEquals(Duration.ofSeconds(60 * 60 * 24), utils.parseDuration("1d"));
        Assertions.assertEquals(Duration.ofSeconds(7 * 60 * 60 * 24), utils.parseDuration("7d"));
        Assertions.assertEquals(
            Duration.ofSeconds(5 * 60 * 60 * 24)
                .plus(Duration.ofSeconds(60 * 60 * 3))
                .plus(Duration.ofSeconds(60 * 18))
                .plus(Duration.ofSeconds(3)),
            utils.parseDuration("5d 3h 18m 3s")
        );
    }

    @Test
    void durationToStr() {
        Assertions.assertEquals("29s", utils.durationToStr(Duration.ofSeconds(29)));
        Assertions.assertEquals("1m", utils.durationToStr(Duration.ofSeconds(60)));
        Assertions.assertEquals("16m", utils.durationToStr(Duration.ofSeconds(16 * 60)));
        Assertions.assertEquals("1h", utils.durationToStr(Duration.ofSeconds(60 * 60)));
        Assertions.assertEquals("1d 8h", utils.durationToStr(Duration.ofSeconds(32 * 60 * 60)));
        Assertions.assertEquals("1d", utils.durationToStr(Duration.ofSeconds(60 * 60 * 24)));
        Assertions.assertEquals("7d", utils.durationToStr(Duration.ofSeconds(7 * 60 * 60 * 24)));
        Assertions.assertEquals(
            "5d 3h 18m 3s",
            utils.durationToStr(
                Duration.ofSeconds(5 * 60 * 60 * 24)
                    .plus(Duration.ofSeconds(60 * 60 * 3))
                    .plus(Duration.ofSeconds(60 * 18))
                    .plus(Duration.ofSeconds(3))
            )
        );
    }
}