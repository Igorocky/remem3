package org.igye.remem3.app.state;

import org.igye.remem3.html.HtmlElem;

public interface StateRenderer<T> {
    HtmlElem render(T state);
}
