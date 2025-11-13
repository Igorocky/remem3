package org.igye.remem3.app.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.igye.remem3.app.RepeatStrategyType;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.fillgaps.TextPart;
import org.igye.remem3.utils.impl.UtilsImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

class CardUtilsImplTest {
    @Test
    void parseText() {
        CardUtilsImpl cards = new CardUtilsImpl(new UtilsImpl(new ObjectMapper()), SettingsImpl.builder().build());
        Assertions.assertEquals(
            List.of(TextPart.Text.builder().text("abc def ghi").build()),
            cards.parseText("abc def ghi")
        );
        Assertions.assertEquals(
            List.of(TextPart.Text.builder().text("abc").build()),
            cards.parseText("\t\n  abc\n\n   \t")
        );
        Assertions.assertEquals(
            List.of(TextPart.Text.builder().text("abc [[def ghi").build()),
            cards.parseText("abc [[def ghi")
        );
        Assertions.assertEquals(
            List.of(
                TextPart.Text.builder().text("abc").build(),
                TextPart.Gap.builder().answer("def").hint("").notes("").build(),
                TextPart.Text.builder().text("ghi").build()
            ),
            cards.parseText("abc [[def]] ghi")
        );
        Assertions.assertEquals(
            List.of(
                TextPart.Text.builder().text("abc").build(),
                TextPart.Gap.builder().answer("def").hint("").notes("").build(),
                TextPart.Text.builder().text("ghi").build(),
                TextPart.Gap.builder().answer("jkl").hint("123").notes("456").build(),
                TextPart.Text.builder().text("mno").build()
            ),
            cards.parseText("abc [[def]] ghi [[jkl|123|456]] mno")
        );
        Assertions.assertEquals(
            List.of(
                TextPart.Gap.builder().answer("def").hint("").notes("").build(),
                TextPart.Text.builder().text("ghi").build(),
                TextPart.Gap.builder().answer("jkl").hint("123").notes("456").build(),
                TextPart.Text.builder().text("mno").build()
            ),
            cards.parseText("[[def]] ghi [[jkl|123|456]] mno")
        );
        Assertions.assertEquals(
            List.of(
                TextPart.Text.builder().text("abc").build(),
                TextPart.Gap.builder().answer("def").hint("").notes("").build(),
                TextPart.Text.builder().text("ghi").build(),
                TextPart.Gap.builder().answer("jkl").hint("123").notes("456").build()
            ),
            cards.parseText("abc [[def]] ghi [[jkl|123|456]]")
        );
    }

    @Test
    void parseHistoryRec() {
        CardUtilsImpl cards = new CardUtilsImpl(new UtilsImpl(new ObjectMapper()), SettingsImpl.builder().build());
        Assertions.assertEquals(
            HistRec.builder()
                .time(Instant.parse("2025-07-04T14:14:08Z"))
                .strategy(RepeatStrategyType.CIRCLE)
                .taskType("task1")
                .mark(BigDecimal.ONE)
                .notes("notes")
                .build(),
            cards.parseHistoryRec("2025-07-04T14:14:08Z CIRCLE task1 1 notes")
        );
        Assertions.assertEquals(
            HistRec.builder()
                .time(Instant.parse("2025-07-04T14:14:08Z"))
                .strategy(RepeatStrategyType.QUEUE)
                .taskType("task1")
                .mark(BigDecimal.ONE)
                .notes("")
                .build(),
            cards.parseHistoryRec("2025-07-04T14:14:08Z QUEUE task1 1 ")
        );
        Assertions.assertEquals(
            HistRec.builder()
                .time(Instant.parse("2025-07-04T14:14:08Z"))
                .strategy(RepeatStrategyType.BUCKETS)
                .taskType("task1")
                .mark(BigDecimal.ZERO)
                .notes("")
                .build(),
            cards.parseHistoryRec("2025-07-04T14:14:08Z BUCKETS task1 0 ")
        );
    }

    @Test
    void parseHistory() {
        CardUtilsImpl cards = new CardUtilsImpl(new UtilsImpl(new ObjectMapper()), SettingsImpl.builder().build());
        Assertions.assertEquals(
            List.of(
                HistRec.builder()
                    .time(Instant.parse("2025-07-04T14:14:08Z"))
                    .strategy(RepeatStrategyType.CIRCLE)
                    .taskType("task1")
                    .mark(BigDecimal.ONE)
                    .notes("notes")
                    .build()
            ),
            cards.parseHistory(List.of("2025-07-04T14:14:08Z CIRCLE task1 1 notes"))
        );
        Assertions.assertEquals(
            List.of(
                HistRec.builder()
                    .time(Instant.parse("2025-07-04T14:14:08Z"))
                    .strategy(RepeatStrategyType.BUCKETS)
                    .taskType("task1")
                    .mark(BigDecimal.ONE)
                    .notes("notes")
                    .build()
            ),
            cards.parseHistory(List.of("\r\n\n2025-07-04T14:14:08Z BUCKETS task1 1 notes\r\n\r\n"))
        );
        Assertions.assertEquals(
            List.of(
                HistRec.builder()
                    .time(Instant.parse("2025-07-03T14:14:08Z"))
                    .strategy(RepeatStrategyType.QUEUE)
                    .taskType("task1")
                    .mark(BigDecimal.ZERO)
                    .notes("notes")
                    .build(),
                HistRec.builder()
                    .time(Instant.parse("2025-07-04T14:14:08Z"))
                    .strategy(RepeatStrategyType.CIRCLE)
                    .taskType("task1")
                    .mark(BigDecimal.ONE)
                    .notes("notes")
                    .build()
            ),
            cards.parseHistory(List.of(
                "\r\n\n2025-07-03T14:14:08Z QUEUE task1 0 notes\r\n",
                "\n2025-07-04T14:14:08Z CIRCLE task1 1 notes\r\n\r\n"
            ))
        );
    }

    @Test
    void parseFillGapsCard_full() {
        CardUtilsImpl cards = new CardUtilsImpl(new UtilsImpl(new ObjectMapper()), SettingsImpl.builder().build());
        Card.FillGaps card = Card.FillGaps.builder()
            .createdAt(Optional.of(Instant.now().truncatedTo(ChronoUnit.SECONDS)))
            .lang("Lang1")
            .descr("Description")
            .text(
                List.of(
                    TextPart.Text.builder().text("abc").build(),
                    TextPart.Gap.builder().answer("def").hint("").notes("").build(),
                    TextPart.Text.builder().text("ghi").build(),
                    TextPart.Gap.builder().answer("jkl").hint("123").notes("456").build(),
                    TextPart.Text.builder().text("mno").build()
                )
            )
            .notes("notes3")
            .history(
                List.of(
                    HistRec.builder()
                        .time(Instant.parse("2025-07-03T14:14:08Z"))
                        .strategy(RepeatStrategyType.QUEUE)
                        .taskType("task1")
                        .mark(BigDecimal.ZERO)
                        .notes("notes")
                        .build(),
                    HistRec.builder()
                        .time(Instant.parse("2025-07-04T14:14:08Z"))
                        .strategy(RepeatStrategyType.BUCKETS)
                        .taskType("task1")
                        .mark(BigDecimal.ONE)
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
        CardUtilsImpl cards = new CardUtilsImpl(new UtilsImpl(new ObjectMapper()), SettingsImpl.builder().build());
        Card.FillGaps card = Card.FillGaps.builder().build();

        Assertions.assertEquals(
            card,
            cards.parseFillGapsCard(cards.fillGapsCardToString(card), Optional.empty())
        );
    }

    @Test
    void validateCard_CardFillGaps() {
        CardUtilsImpl cards = new CardUtilsImpl(
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
            .text(List.of(TextPart.Text.builder().text(" ").build()))
            .notes("123")
            .history(List.of(HistRec.builder().build()))
            .build();

        Assertions.assertEquals(
            List.of("Text is empty."),
            cards.validateCard(card)
        );

        card = Card.FillGaps.builder()
            .lang("EN")
            .text(List.of(TextPart.Text.builder().text(".").build()))
            .notes("123")
            .history(List.of(HistRec.builder().build()))
            .build();

        Assertions.assertEquals(
            List.of("At least one gap must be defined."),
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

        card = Card.FillGaps.builder()
            .lang("EN")
            .text(cards.parseText("aaa [[111]] bbb"))
            .notes("123")
            .history(List.of(HistRec.builder().build()))
            .build();

        Assertions.assertEquals(
            List.of(),
            cards.validateCard(card)
        );
    }

    @Test
    void validateCard_CardTranslate() {
        CardUtilsImpl cards = new CardUtilsImpl(
            new UtilsImpl(new ObjectMapper()),
            SettingsImpl.builder().languages(List.of("EN", "PL")).build()
        );

        Card.Translate card = Card.Translate.builder().build();

        Assertions.assertEquals(
            List.of(
                "Language1 is not set.", "Text1 is not set.", "Language2 is not set.", "Text2 is not set.",
                "Languages must be different."
            ),
            cards.validateCard(card)
        );

        card = Card.Translate.builder()
            .lang1("ABC")
            .text1("111")
            .lang2("DEF")
            .text2("222")
            .build();

        Assertions.assertEquals(
            List.of("Language1 'ABC' is not registered.", "Language2 'DEF' is not registered."),
            cards.validateCard(card)
        );

        card = Card.Translate.builder()
            .lang1("EN")
            .text1("111")
            .lang2("PL")
            .text2("222")
            .build();

        Assertions.assertEquals(
            List.of(),
            cards.validateCard(card)
        );

    }

    @Test
    void histRecToStr() {
        CardUtilsImpl cards = new CardUtilsImpl(
            new UtilsImpl(new ObjectMapper()),
            SettingsImpl.builder().languages(List.of("EN")).build()
        );
        Assertions.assertEquals(
            "2025-07-04T14:14:08Z QUEUE task-type-123 0.5 NOTES-ABC",
            cards.histRecToStr(
                HistRec.builder()
                    .time(Instant.parse("2025-07-04T14:14:08.038Z"))
                    .strategy(RepeatStrategyType.QUEUE)
                    .taskType("task-type-123")
                    .mark(new BigDecimal("0.5"))
                    .notes("NOTES-ABC")
                    .build()
            )
        );
    }
}