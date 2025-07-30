package org.igye.remem3.utils;

public interface Producer<T> {
    T get() throws Exception;
}
