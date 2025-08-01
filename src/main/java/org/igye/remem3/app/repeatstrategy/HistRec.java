package org.igye.remem3.app.repeatstrategy;

import java.math.BigDecimal;
import java.time.Instant;

public interface HistRec {
    Instant getTime();

    BigDecimal getMark();

    default boolean isPassed() {
        return BigDecimal.ONE.compareTo(getMark()) <= 0;
    }
}
