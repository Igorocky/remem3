package org.igye.remem3.app.repeatstrategy;

import java.util.List;

public interface Hist {
    List<HistRec> getRecords();

    int getBucketIdx(int maxBucketIdx);
}
