package org.igye.remem3.app.impl;

import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.fillgaps.Gap;
import org.igye.remem3.app.dto.fillgaps.Text;
import org.igye.remem3.utils.impl.UtilsImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

class CardsImplTest {
    @Test
    void parseText() {
        CardsImpl cards = new CardsImpl(new UtilsImpl());
        Assertions.assertEquals(
            List.of(Text.builder().text("abc def ghi").build()),
            cards.parseText("abc def ghi")
        );
        Assertions.assertEquals(
            List.of(Text.builder().text("abc").build()),
            cards.parseText("\t\n  abc\n\n   \t")
        );
        Assertions.assertEquals(
            List.of(Text.builder().text("abc [[def ghi").build()),
            cards.parseText("abc [[def ghi")
        );
        Assertions.assertEquals(
            List.of(
                Text.builder().text("abc").build(),
                Gap.builder().answer("def").hint("").notes("").build(),
                Text.builder().text("ghi").build()
            ),
            cards.parseText("abc [[def]] ghi")
        );
        Assertions.assertEquals(
            List.of(
                Text.builder().text("abc").build(),
                Gap.builder().answer("def").hint("").notes("").build(),
                Text.builder().text("ghi").build(),
                Gap.builder().answer("jkl").hint("123").notes("456").build(),
                Text.builder().text("mno").build()
            ),
            cards.parseText("abc [[def]] ghi [[jkl|123|456]] mno")
        );
        Assertions.assertEquals(
            List.of(
                Gap.builder().answer("def").hint("").notes("").build(),
                Text.builder().text("ghi").build(),
                Gap.builder().answer("jkl").hint("123").notes("456").build(),
                Text.builder().text("mno").build()
            ),
            cards.parseText("[[def]] ghi [[jkl|123|456]] mno")
        );
        Assertions.assertEquals(
            List.of(
                Text.builder().text("abc").build(),
                Gap.builder().answer("def").hint("").notes("").build(),
                Text.builder().text("ghi").build(),
                Gap.builder().answer("jkl").hint("123").notes("456").build()
            ),
            cards.parseText("abc [[def]] ghi [[jkl|123|456]]")
        );
    }

    @Test
    void parseHistoryRec() {
        CardsImpl cards = new CardsImpl(new UtilsImpl());
        Assertions.assertEquals(
            HistRec.builder()
                .time(Instant.parse("2025-07-04T14:14:08Z"))
                .taskType("task1")
                .mark(1.0)
                .notes("notes")
                .build(),
            cards.parseHistoryRec("2025-07-04T14:14:08Z task1 1.0 notes")
        );
        Assertions.assertEquals(
            HistRec.builder()
                .time(Instant.parse("2025-07-04T14:14:08Z"))
                .taskType("task1")
                .mark(1.0)
                .notes("")
                .build(),
            cards.parseHistoryRec("2025-07-04T14:14:08Z task1 1.0 ")
        );
        Assertions.assertEquals(
            HistRec.builder()
                .time(Instant.parse("2025-07-04T14:14:08Z"))
                .taskType("task1")
                .mark(0.0)
                .notes("")
                .build(),
            cards.parseHistoryRec("2025-07-04T14:14:08Z task1 0 ")
        );
    }

    @Test
    void parseHistory() {
        CardsImpl cards = new CardsImpl(new UtilsImpl());
        Assertions.assertEquals(
            List.of(
                HistRec.builder()
                    .time(Instant.parse("2025-07-04T14:14:08Z"))
                    .taskType("task1")
                    .mark(1.0)
                    .notes("notes")
                    .build()
            ),
            cards.parseHistory("2025-07-04T14:14:08Z task1 1.0 notes")
        );
        Assertions.assertEquals(
            List.of(
                HistRec.builder()
                    .time(Instant.parse("2025-07-04T14:14:08Z"))
                    .taskType("task1")
                    .mark(1.0)
                    .notes("notes")
                    .build()
            ),
            cards.parseHistory("\r\n\n2025-07-04T14:14:08Z task1 1.0 notes\r\n\r\n")
        );
        Assertions.assertEquals(
            List.of(
                HistRec.builder()
                    .time(Instant.parse("2025-07-03T14:14:08Z"))
                    .taskType("task1")
                    .mark(0.0)
                    .notes("notes")
                    .build(),
                HistRec.builder()
                    .time(Instant.parse("2025-07-04T14:14:08Z"))
                    .taskType("task1")
                    .mark(1.0)
                    .notes("notes")
                    .build()
            ),
            cards.parseHistory(
                "\r\n\n2025-07-03T14:14:08Z task1 0.0 notes\r\n\n2025-07-04T14:14:08Z task1 1.0 notes\r\n\r\n"
            )
        );
    }
}