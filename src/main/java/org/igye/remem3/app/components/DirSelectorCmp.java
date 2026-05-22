package org.igye.remem3.app.components;

import org.igye.remem3.html.HtmlElem;

import java.io.File;

public interface DirSelectorCmp {
    String getSelectedDirectoryStr();

    File getSelectedDirectory();

    HtmlElem render();
}
