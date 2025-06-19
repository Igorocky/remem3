package org.igye.remem3.utils;

public interface Func<I, O> {
    O apply(I input) throws Exception;
}
