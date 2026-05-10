package org.igye.remem3.app.controllers.convertfillgapstotrnaslate;

import org.igye.remem3.app.dto.Card;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.igye.remem3.app.controllers.convertfillgapstotrnaslate.ConvertFillGapsToTranslateRenderer.ATTR_AUTO_GENERATED_FROM;

class ConvertFillGapsToTranslateConstructorTest {
    @Test
    void makeKeyForExistingCard() {
        //given
        ConvertFillGapsToTranslateConstructor constructor = new ConvertFillGapsToTranslateConstructor(null, null, null);

        //when/then
        Assertions.assertEquals(
            Optional.of(NewCardKey.builder().origFileUniqueName("unique_file_name.fg.card").gapAns("ans1").build()),
            constructor.makeKeyForExistingCard(
                Card.Translate.builder()
                    .attrs(Map.of(
                        ATTR_AUTO_GENERATED_FROM,
                        "unique_file_name.fg.card:ans1"
                    ))
                    .build()
            )
        );
    }

    @Test
    void newCardKeyFromString() {
        //given
        ConvertFillGapsToTranslateConstructor constructor = new ConvertFillGapsToTranslateConstructor(null, null, null);

        //when/then
        Assertions.assertEquals(
            Optional.of(NewCardKey.builder().origFileUniqueName("unique_file_name.fg.card").gapAns("ans1").build()),
            constructor.newCardKeyFromString("unique_file_name.fg.card:ans1")
        );
        Assertions.assertEquals(
            Optional.of(NewCardKey.builder().origFileUniqueName("unique_file_name.fg.card").gapAns("an:s1").build()),
            constructor.newCardKeyFromString("unique_file_name.fg.card:an:s1")
        );
        Assertions.assertEquals(Optional.empty(), constructor.newCardKeyFromString(null));
        Assertions.assertEquals(Optional.empty(), constructor.newCardKeyFromString(""));
        Assertions.assertEquals(Optional.empty(), constructor.newCardKeyFromString("a"));
        Assertions.assertEquals(Optional.empty(), constructor.newCardKeyFromString(":"));
        Assertions.assertEquals(Optional.empty(), constructor.newCardKeyFromString("a:"));
        Assertions.assertEquals(Optional.empty(), constructor.newCardKeyFromString(":a"));
        Assertions.assertEquals(
            Optional.of(NewCardKey.builder().origFileUniqueName("a").gapAns(":").build()),
            constructor.newCardKeyFromString("a::")
        );
        Assertions.assertEquals(
            Optional.of(NewCardKey.builder().origFileUniqueName("a").gapAns("b").build()),
            constructor.newCardKeyFromString(" a : b ")
        );
    }

}