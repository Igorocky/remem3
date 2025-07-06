package org.igye.remem3.controllers.components;

import org.igye.remem3.html.HtmlElem;

import java.util.List;

public interface DirSelectorCmp {
    List<String> getSelectedDirectory();

    HtmlElem render();
}
