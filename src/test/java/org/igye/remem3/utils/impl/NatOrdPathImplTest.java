package org.igye.remem3.utils.impl;

import org.igye.remem3.utils.NatOrdString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;

class NatOrdPathImplTest {
    @Test
    void getParts_returns_correct_result() {
        Assertions.assertArrayEquals(
            new String[]{"abc", "ghi", "123", "456"},
            Arrays.stream(new NatOrdPathImpl(new File("abc/def/../ghi/./123/456")).getParts())
                .map(NatOrdString::getValue)
                .toArray()
        );
        Assertions.assertArrayEquals(
            new String[]{"abc", "ghi", "123", "456"},
            Arrays.stream(new NatOrdPathImpl(new File("/abc/def/../ghi/./123/456")).getParts())
                .map(NatOrdString::getValue)
                .toArray()
        );
    }

    @Test
    void compareTo_compares_paths_correctly() {
        assertIsLess(
            new File("Chapter 1/2.1"),
            new File("Chapter 1/11.1")
        );
        assertIsLess(
            new File("Chapter 1/2.1"),
            new File("Chapter 1/2.1/2.1.1")
        );
    }

    private void assertIsLess(File file1, File file2) {
        Assertions.assertEquals(
            -1,
            new NatOrdPathImpl(file1).compareTo(new NatOrdPathImpl(file2))
        );
    }

}