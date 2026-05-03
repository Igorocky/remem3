package org.igye.remem3.app.state;

public interface StateRenderer<T> {
    String render(T state);
}
