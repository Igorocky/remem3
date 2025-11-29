package org.igye.remem3.app.controllers.convertfillgapstotrnaslate;

import org.igye.remem3.app.dto.Card;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

class ConvertFillGapsToTranslateControllerTest {
    @Test
    void makeKeyForExistingCard() {
        //given
        ConvertFillGapsToTranslateController controller = new ConvertFillGapsToTranslateController(null, null, null);

        //when/then
        Assertions.assertEquals(
            Optional.of(NewCardKey.builder().origFileUniqueName("unique_file_name.fg.card").gapAns("ans1").build()),
            controller.makeKeyForExistingCard(
                Card.Translate.builder()
                    .attrs(Map.of(
                        ConvertFillGapsToTranslateController.ATTR_AUTO_GENERATED_FROM,
                        "unique_file_name.fg.card:ans1"
                    ))
                    .build()
            )
        );
    }

    @Test
    void newCardKeyFromString() {
        //given
        ConvertFillGapsToTranslateController controller = new ConvertFillGapsToTranslateController(null, null, null);

        //when/then
        Assertions.assertEquals(
            Optional.of(NewCardKey.builder().origFileUniqueName("unique_file_name.fg.card").gapAns("ans1").build()),
            controller.newCardKeyFromString("unique_file_name.fg.card:ans1")
        );
        Assertions.assertEquals(
            Optional.of(NewCardKey.builder().origFileUniqueName("unique_file_name.fg.card").gapAns("an:s1").build()),
            controller.newCardKeyFromString("unique_file_name.fg.card:an:s1")
        );
        Assertions.assertEquals(Optional.empty(), controller.newCardKeyFromString(null));
        Assertions.assertEquals(Optional.empty(), controller.newCardKeyFromString(""));
        Assertions.assertEquals(Optional.empty(), controller.newCardKeyFromString("a"));
        Assertions.assertEquals(Optional.empty(), controller.newCardKeyFromString(":"));
        Assertions.assertEquals(Optional.empty(), controller.newCardKeyFromString("a:"));
        Assertions.assertEquals(Optional.empty(), controller.newCardKeyFromString(":a"));
        Assertions.assertEquals(
            Optional.of(NewCardKey.builder().origFileUniqueName("a").gapAns(":").build()),
            controller.newCardKeyFromString("a::")
        );
        Assertions.assertEquals(
            Optional.of(NewCardKey.builder().origFileUniqueName("a").gapAns("b").build()),
            controller.newCardKeyFromString(" a : b ")
        );
    }

}