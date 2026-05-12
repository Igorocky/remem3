package org.igye.remem3.utils.impl;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NatOrdStringImplTest {
    @Test
    void NatOrdStringImpl_splits_string_into_parts_correctly() {
        NatOrdStringImpl emptyStr = new NatOrdStringImpl("");
        assertArrayEquals(new String[]{}, emptyStr.getLeft());
        assertEquals("", emptyStr.getMiddle());
        assertEquals("", emptyStr.getRight());

        NatOrdStringImpl middleOnly = new NatOrdStringImpl("abc");
        assertArrayEquals(new String[]{}, middleOnly.getLeft());
        assertEquals("abc", middleOnly.getMiddle());
        assertEquals("", middleOnly.getRight());

        NatOrdStringImpl leftOnly = new NatOrdStringImpl("123");
        assertArrayEquals(new String[]{"123"}, leftOnly.getLeft());
        assertEquals("", leftOnly.getMiddle());
        assertEquals("", leftOnly.getRight());

        NatOrdStringImpl leftAndMiddle = new NatOrdStringImpl("123abc");
        assertArrayEquals(new String[]{"123"}, leftAndMiddle.getLeft());
        assertEquals("abc", leftAndMiddle.getMiddle());
        assertEquals("", leftAndMiddle.getRight());

        NatOrdStringImpl middleAndRight = new NatOrdStringImpl("abc456");
        assertArrayEquals(new String[]{}, middleAndRight.getLeft());
        assertEquals("abc", middleAndRight.getMiddle());
        assertEquals("456", middleAndRight.getRight());

        NatOrdStringImpl allParts = new NatOrdStringImpl("123abc456");
        assertArrayEquals(new String[]{"123"}, allParts.getLeft());
        assertEquals("abc", allParts.getMiddle());
        assertEquals("456", allParts.getRight());

        NatOrdStringImpl leftWithDotsOnly = new NatOrdStringImpl("123.456");
        assertArrayEquals(new String[]{"123", "456"}, leftWithDotsOnly.getLeft());
        assertEquals("", leftWithDotsOnly.getMiddle());
        assertEquals("", leftWithDotsOnly.getRight());

        NatOrdStringImpl leftWithDotsAndMiddle = new NatOrdStringImpl("123.456.abc");
        assertArrayEquals(new String[]{"123", "456"}, leftWithDotsAndMiddle.getLeft());
        assertEquals(".abc", leftWithDotsAndMiddle.getMiddle());
        assertEquals("", leftWithDotsAndMiddle.getRight());

        NatOrdStringImpl allPartsWithDots = new NatOrdStringImpl("123.456 abc789");
        assertArrayEquals(new String[]{"123", "456"}, allPartsWithDots.getLeft());
        assertEquals("abc", allPartsWithDots.getMiddle());
        assertEquals("789", allPartsWithDots.getRight());
    }

    @Test
    void compareTo_compares_as_expected_with_numbers_on_the_left() {
        assertEquals(
            -1,
            new NatOrdStringImpl("2.1").compareTo(new NatOrdStringImpl("11.1"))
        );
        assertEquals(
            -1,
            new NatOrdStringImpl("2.3").compareTo(new NatOrdStringImpl("2.11"))
        );
    }

    @Test
    void compareTo_sorts_as_expected_with_numbers_on_the_left() {
        //given
        List<String> list = List.of("12-file", "1-file", "11-file", "2-file");

        //when
        List<String> sortedList = list.stream()
            .map(NatOrdStringImpl::new)
            .sorted()
            .map(NatOrdStringImpl::getValue)
            .toList();

        //then
        assertEquals(
            List.of("1-file", "2-file", "11-file", "12-file"),
            sortedList
        );

    }

    @Test
    void compareTo_compares_as_expected_with_numbers_on_the_right() {
        //given
        List<String> list = List.of("file12", "file2", "file11", "file1");

        //when
        List<String> sortedList = list.stream()
            .map(NatOrdStringImpl::new)
            .sorted()
            .map(NatOrdStringImpl::getValue)
            .toList();

        //then
        assertEquals(
            List.of("file1", "file2", "file11", "file12"),
            sortedList
        );

    }
}