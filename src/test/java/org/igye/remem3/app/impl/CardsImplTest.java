package org.igye.remem3.app.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.fillgaps.Gap;
import org.igye.remem3.app.dto.fillgaps.Text;
import org.igye.remem3.utils.impl.UtilsImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

class CardsImplTest {
    @Test
    void parseText() {
        CardsImpl cards = new CardsImpl(new UtilsImpl(new ObjectMapper()), SettingsImpl.builder().build());
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
        CardsImpl cards = new CardsImpl(new UtilsImpl(new ObjectMapper()), SettingsImpl.builder().build());
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
        CardsImpl cards = new CardsImpl(new UtilsImpl(new ObjectMapper()), SettingsImpl.builder().build());
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

    @Test
    void parseFillGapsCard_full() {
        CardsImpl cards = new CardsImpl(new UtilsImpl(new ObjectMapper()), SettingsImpl.builder().build());
        Card.FillGaps card = Card.FillGaps.builder()
            .createdAt(Optional.of(Instant.now()))
            .lang("Lang1")
            .text(
                List.of(
                    Text.builder().text("abc").build(),
                    Gap.builder().answer("def").hint("").notes("").build(),
                    Text.builder().text("ghi").build(),
                    Gap.builder().answer("jkl").hint("123").notes("456").build(),
                    Text.builder().text("mno").build()
                )
            )
            .notes("notes3")
            .history(
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
                )
            )
            .build();

        Assertions.assertEquals(
            card,
            cards.parseFillGapsCard(cards.fillGapsCardToString(card), Optional.empty())
        );
    }

    @Test
    void parseFillGapsCard_empty() {
        CardsImpl cards = new CardsImpl(new UtilsImpl(new ObjectMapper()), SettingsImpl.builder().build());
        Card.FillGaps card = Card.FillGaps.builder().build();

        Assertions.assertEquals(
            card,
            cards.parseFillGapsCard(cards.fillGapsCardToString(card), Optional.empty())
        );
    }

    @Test
    void validateCard_CardFillGaps() {
        CardsImpl cards = new CardsImpl(
            new UtilsImpl(new ObjectMapper()),
            SettingsImpl.builder().languages(List.of("EN")).build()
        );

        Card.FillGaps card = Card.FillGaps.builder().build();

        Assertions.assertEquals(
            List.of("Language is not set.", "Text is empty."),
            cards.validateCard(card)
        );

        card = Card.FillGaps.builder()
            .lang("IT")
            .text(List.of())
            .notes("")
            .history(List.of())
            .build();

        Assertions.assertEquals(
            List.of("Language 'IT' is not registered.", "Text is empty."),
            cards.validateCard(card)
        );

        card = Card.FillGaps.builder()
            .lang("EN")
            .text(List.of(Text.builder().text(" ").build()))
            .notes("123")
            .history(List.of(HistRec.builder().build()))
            .build();

        Assertions.assertEquals(
            List.of("Text is empty."),
            cards.validateCard(card)
        );

        card = Card.FillGaps.builder()
            .lang("EN")
            .text(List.of(Text.builder().text(".").build()))
            .notes("123")
            .history(List.of(HistRec.builder().build()))
            .build();

        Assertions.assertEquals(
            List.of(),
            cards.validateCard(card)
        );

        card = Card.FillGaps.builder()
            .lang("EN")
            .text(cards.parseText("aaa [[]] bbb"))
            .notes("123")
            .history(List.of(HistRec.builder().build()))
            .build();

        Assertions.assertEquals(
            List.of("A gap cannot be empty."),
            cards.validateCard(card)
        );

        card = Card.FillGaps.builder()
            .lang("EN")
            .text(cards.parseText("aaa [[   ]] bbb"))
            .notes("123")
            .history(List.of(HistRec.builder().build()))
            .build();

        Assertions.assertEquals(
            List.of("A gap cannot be empty."),
            cards.validateCard(card)
        );
    }
}