package org.igye.remem3.utils;

import java.io.File;

public interface NatOrdPath extends Comparable<NatOrdPath> {
    NatOrdString[] getParts();

    File getValue();
}
