package org.igye.remem3.app.impl;

import org.igye.remem3.app.dto.fillgaps.Gap;
import org.igye.remem3.app.dto.fillgaps.Text;
import org.igye.remem3.utils.impl.UtilsImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class CardsImplTest {
    @Test
    void parseText() {
        Assertions.assertEquals(
            List.of(Text.builder().text("abc def ghi").build()),
            new CardsImpl(new UtilsImpl()).parseText("abc def ghi")
        );
        Assertions.assertEquals(
            List.of(Text.builder().text("abc [[def ghi").build()),
            new CardsImpl(new UtilsImpl()).parseText("abc [[def ghi")
        );
        Assertions.assertEquals(
            List.of(
                Text.builder().text("abc").build(),
                Gap.builder().answer("def").hint("").notes("").build(),
                Text.builder().text("ghi").build()
            ),
            new CardsImpl(new UtilsImpl()).parseText("abc [[def]] ghi")
        );
        Assertions.assertEquals(
            List.of(
                Text.builder().text("abc").build(),
                Gap.builder().answer("def").hint("").notes("").build(),
                Text.builder().text("ghi").build(),
                Gap.builder().answer("jkl").hint("123").notes("456").build(),
                Text.builder().text("mno").build()
            ),
            new CardsImpl(new UtilsImpl()).parseText("abc [[def]] ghi [[jkl|123|456]] mno")
        );
        Assertions.assertEquals(
            List.of(
                Gap.builder().answer("def").hint("").notes("").build(),
                Text.builder().text("ghi").build(),
                Gap.builder().answer("jkl").hint("123").notes("456").build(),
                Text.builder().text("mno").build()
            ),
            new CardsImpl(new UtilsImpl()).parseText("[[def]] ghi [[jkl|123|456]] mno")
        );
        Assertions.assertEquals(
            List.of(
                Text.builder().text("abc").build(),
                Gap.builder().answer("def").hint("").notes("").build(),
                Text.builder().text("ghi").build(),
                Gap.builder().answer("jkl").hint("123").notes("456").build()
            ),
            new CardsImpl(new UtilsImpl()).parseText("abc [[def]] ghi [[jkl|123|456]]")
        );
    }
}