package org.igye.remem3.test;

import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.web.RequestParams;

public interface TestUtils {
    void assertInputs(HtmlElem html, HtmlTag... elems);

    void setValue(HtmlElem html, String paramName, String value);

    RequestParams submit(HtmlElem html, String submitButtonName);
}
