package org.igye.remem3.app.state;

public interface StateConstructor<T> {
    String getName();

    T construct();

    boolean isSingleton();
}
