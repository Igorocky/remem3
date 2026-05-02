package org.igye.remem3.app.state;

public interface StateConstructor<T> extends TypeSupporter {
    String getName();

    T construct();

    boolean isSingleton();
}
