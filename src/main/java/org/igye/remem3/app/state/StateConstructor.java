package org.igye.remem3.app.state;

public interface StateConstructor<T> {
    String getName();

    String getDisplayName();

    T construct();

    boolean isSingleton();
}
