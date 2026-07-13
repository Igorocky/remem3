package org.igye.remem3.app.repeatstrategy.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.repeatstrategy.HistRec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

class HistImplTest {
    @Test
    void getBucketIdx_calculates_correct_bucket_idx() {
        testBucketIdx(new int[]{}, 3, 3);

        testBucketIdx(new int[]{1}, 3, 3);
        testBucketIdx(new int[]{0}, 3, 0);

        testBucketIdx(new int[]{1, 1}, 3, 3);
        testBucketIdx(new int[]{1, 1, 1}, 3, 3);
        testBucketIdx(new int[]{1, 0}, 3, 2);
        testBucketIdx(new int[]{1, 0, 1}, 3, 3);
        testBucketIdx(new int[]{1, 0, 1, 0}, 3, 2);
        testBucketIdx(new int[]{1, 0, 1, 0, 0}, 3, 1);
        testBucketIdx(new int[]{1, 0, 1, 0, 0, 0}, 3, 0);
        testBucketIdx(new int[]{1, 0, 1, 0, 0, 0, 0}, 3, 0);
        testBucketIdx(new int[]{1, 0, 1, 0, 0, 0, 0, 0, 0, 1}, 3, 1);
        testBucketIdx(new int[]{1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1}, 3, 2);
        testBucketIdx(new int[]{1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1}, 3, 3);
        testBucketIdx(new int[]{1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1}, 3, 3);
        testBucketIdx(new int[]{1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0}, 3, 2);
    }

    @Test
    void getBucketIdx_recalculates_bucket_idx_when_maxBucketIdx_changes() {
        HistImpl hist = new HistImpl(makeHist(new int[]{1, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1}));
        Assertions.assertEquals(3, hist.getBucketIdx(3));
        Assertions.assertEquals(4, hist.getBucketIdx(4));
        Assertions.assertEquals(3, hist.getBucketIdx(3));
    }

    private void testBucketIdx(int[] results, int maxBucketIdx, int expectedBucketIdx) {
        Assertions.assertEquals(expectedBucketIdx, new HistImpl(makeHist(results)).getBucketIdx(maxBucketIdx));
    }

    private List<HistRec> makeHist(int[] results) {
        Instant time = Instant.now().minusSeconds(1000);
        ArrayList<HistRec> res = new ArrayList<>();
        for (int passed : results) {
            res.add(new TestHistRec(time, passed == 0 ? BigDecimal.ZERO : BigDecimal.ONE));
            time = time.plusSeconds(1);
        }
        return res;
    }

    @RequiredArgsConstructor
    @Getter
    private static class TestHistRec implements HistRec {
        private final Instant time;
        private final BigDecimal mark;
    }

}