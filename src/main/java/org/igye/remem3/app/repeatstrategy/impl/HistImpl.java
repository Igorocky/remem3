package org.igye.remem3.app.repeatstrategy.impl;

import org.igye.remem3.app.repeatstrategy.Hist;
import org.igye.remem3.app.repeatstrategy.HistRec;

import java.util.Collections;
import java.util.List;

public class HistImpl implements Hist {
    private final List<HistRec> hist;

    private int maxBucketIdx;
    private int bucketIdx = -1;

    public HistImpl(List<HistRec> hist) {
        this.hist = Collections.unmodifiableList(hist);
    }

    @Override
    public List<HistRec> getRecords() {
        return hist;
    }

    @Override
    public int getBucketIdx(int maxBucketIdx) {
        if (this.maxBucketIdx != maxBucketIdx || bucketIdx < 0) {
            this.maxBucketIdx = maxBucketIdx;
            for (HistRec histRec : hist) {
                if (histRec.isPassed()) {
                    if (bucketIdx < 0) {
                        bucketIdx = maxBucketIdx;
                    } else {
                        bucketIdx = Math.min(bucketIdx + 1, maxBucketIdx);
                    }
                } else {
                    if (bucketIdx < 0) {
                        bucketIdx = 0;
                    } else {
                        bucketIdx = Math.max(bucketIdx - 1, 0);
                    }
                }
            }
            if (bucketIdx < 0) {
                bucketIdx = maxBucketIdx;
            }
        }
        return bucketIdx;
    }
}
