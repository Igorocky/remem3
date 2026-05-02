package org.igye.remem3.app.state;

public interface StateRenderer<T> extends TypeSupporter {
    String render(T state);
}
