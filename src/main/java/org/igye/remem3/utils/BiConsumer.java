package org.igye.remem3.utils;

public interface BiConsumer<A, B> {
    void consume(A input1, B input2) throws Exception;
}
