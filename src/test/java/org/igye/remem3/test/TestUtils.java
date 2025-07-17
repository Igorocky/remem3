package org.igye.remem3.test;

import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.web.RequestParams;

import java.util.List;

public interface TestUtils {
    void assertInputs(HtmlElem html, HtmlTag... elems);

    void assertInputs(HtmlElem html, List<HtmlTag> elems);

    void setValue(HtmlElem html, String paramName, String value);

    void setValueExn(HtmlElem html, String paramName, String value);

    RequestParams submit(HtmlElem html, String submitButtonName);
}
