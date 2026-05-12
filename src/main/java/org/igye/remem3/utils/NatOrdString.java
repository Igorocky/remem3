package org.igye.remem3.utils;

import java.math.BigDecimal;

public interface NatOrdString extends Comparable<NatOrdString> {
    String[] getLeft();

    BigDecimal[] getLeftNum();

    String getMiddle();

    String getRight();

    BigDecimal getRightNum();

    String getValue();
}
