package org.igye.remem3.utils.impl;

import org.igye.remem3.app.repeatstrategy.HistRec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
        Assertions.assertEquals(
            "5d 3h 18m 3s",
            utils.durationToStr(
                Duration.ofSeconds(5 * 60 * 60 * 24)
                    .plus(Duration.ofSeconds(60 * 60 * 3))
                    .plus(Duration.ofSeconds(60 * 18))
                    .plus(Duration.ofSeconds(3)),
                4
            )
        );
        Assertions.assertEquals(
            "5d 3h 18m",
            utils.durationToStr(
                Duration.ofSeconds(5 * 60 * 60 * 24)
                    .plus(Duration.ofSeconds(60 * 60 * 3))
                    .plus(Duration.ofSeconds(60 * 18))
                    .plus(Duration.ofSeconds(3)),
                3
            )
        );
        Assertions.assertEquals(
            "5d 3h",
            utils.durationToStr(
                Duration.ofSeconds(5 * 60 * 60 * 24)
                    .plus(Duration.ofSeconds(60 * 60 * 3))
                    .plus(Duration.ofSeconds(60 * 18))
                    .plus(Duration.ofSeconds(3)),
                2
            )
        );
        Assertions.assertEquals(
            "5d",
            utils.durationToStr(
                Duration.ofSeconds(5 * 60 * 60 * 24)
                    .plus(Duration.ofSeconds(60 * 60 * 3))
                    .plus(Duration.ofSeconds(60 * 18))
                    .plus(Duration.ofSeconds(3)),
                1
            )
        );
        Assertions.assertEquals(
            "3h",
            utils.durationToStr(
                Duration.ofSeconds(60 * 60 * 3)
                    .plus(Duration.ofSeconds(60 * 18))
                    .plus(Duration.ofSeconds(3)),
                1
            )
        );
    }

    @Test
    void replacePlaceholders() {
        Assertions.assertEquals("123", utils.replacePlaceholders("123", _ -> ""));
        Assertions.assertEquals(
            "A123",
            utils.replacePlaceholders("${a}123", Map.of("a", "A")::get)
        );
        Assertions.assertEquals(
            "1A23",
            utils.replacePlaceholders("1${a}23", Map.of("a", "A")::get)
        );
        Assertions.assertEquals(
            "123A",
            utils.replacePlaceholders("123${a}", Map.of("a", "A")::get)
        );
        Assertions.assertEquals(
            "A1B2C3D",
            utils.replacePlaceholders("${a}1${b}2${c}3${d}", Map.of("a", "A", "b", "B", "c", "C", "d", "D")::get)
        );
        Assertions.assertEquals(
            "ABC1DEF2GHI",
            utils.replacePlaceholders("${x1}1${x2}2${x3}", Map.of("x1", "ABC", "x2", "DEF", "x3", "GHI")::get)
        );
    }

    @Test
    void getStreak() {
        Assertions.assertEquals(
            0,
            utils.getStreak(makeHist(BigDecimal.ZERO))
        );
        Assertions.assertEquals(
            0,
            utils.getStreak(makeHist(BigDecimal.ZERO, BigDecimal.ZERO))
        );
        Assertions.assertEquals(
            0,
            utils.getStreak(makeHist(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
        );
        Assertions.assertEquals(
            0,
            utils.getStreak(makeHist(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO))
        );
        Assertions.assertEquals(
            0,
            utils.getStreak(makeHist(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO))
        );
        Assertions.assertEquals(
            0,
            utils.getStreak(makeHist(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO))
        );
        Assertions.assertEquals(
            1,
            utils.getStreak(makeHist(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE))
        );
        Assertions.assertEquals(
            2,
            utils.getStreak(makeHist(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE))
        );
        Assertions.assertEquals(
            3,
            utils.getStreak(makeHist(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE))
        );
        Assertions.assertEquals(
            2,
            utils.getStreak(makeHist(BigDecimal.ONE, BigDecimal.ONE))
        );
        Assertions.assertEquals(
            1,
            utils.getStreak(makeHist(BigDecimal.ONE))
        );
    }

    private List<HistRec> makeHist(BigDecimal... marks) {
        return Arrays.stream(marks)
            .map(mark ->
                (HistRec) org.igye.remem3.app.dto.HistRec.builder()
                    .mark(mark)
                    .time(Instant.now())
                    .build()
            )
            .toList();
    }

}