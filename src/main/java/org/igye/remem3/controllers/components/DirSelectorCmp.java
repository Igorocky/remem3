package org.igye.remem3.controllers.components;

import org.igye.remem3.html.HtmlElem;

import java.io.File;
import java.util.List;

public interface DirSelectorCmp {
    List<String> getSelectedDirectoryList();

    String getSelectedDirectoryStr();

    File getSelectedDirectory();

    HtmlElem render();
}
