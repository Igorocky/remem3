package org.igye.remem3.app.components;

import org.igye.remem3.html.HtmlElem;

import java.util.Set;

public interface PrioritySelectorCmp {
    Set<Integer> getSelectedPriorities();

    HtmlElem render();
}
