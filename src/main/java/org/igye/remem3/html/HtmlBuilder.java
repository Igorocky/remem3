package org.igye.remem3.html;

import org.apache.commons.text.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HtmlBuilder {
    protected HtmlText text(String text) {
        return new HtmlText(esc(text));
    }

    protected HtmlTag h(String tagName, Map<String, String> attrs, HtmlElem... children) {
        return new HtmlTag(tagName, attrs, childrenArrayToList(children));
    }

    protected HtmlTag h(String tagName, Map<String, String> attrs, List<? extends HtmlElem> children) {
        return new HtmlTag(tagName, attrs, children);
    }

    protected HtmlTag h(String tagName, Map<String, String> attrs) {
        return new HtmlTag(tagName, attrs, null);
    }

    protected HtmlTag h(String tagName, HtmlElem... children) {
        return new HtmlTag(tagName, null, childrenArrayToList(children));
    }

    protected HtmlTag h(String tagName, List<? extends HtmlElem> children) {
        return new HtmlTag(tagName, null, children);
    }

    protected HtmlTag h(String tagName) {
        return new HtmlTag(tagName, null, null);
    }

    protected String esc(String text) {
        return StringEscapeUtils.escapeHtml4(text);
    }

    protected HtmlFragment frag(HtmlElem... children) {
        return new HtmlFragment(childrenArrayToList(children));
    }

    protected HtmlFragment frag(List<HtmlElem> children) {
        return new HtmlFragment(children);
    }

    protected HtmlTag simplePageWithTitle(String title, HtmlElem... content) {
        return simplePageWithTitle(title, childrenArrayToList(content));
    }

    protected HtmlTag simplePageWithTitle(String title, List<? extends HtmlElem> content) {
        return h("html",
            h("head",
                h("title", text(title))
            ),
            h("body",
                content
            )
        );
    }

    private <T> List<T> childrenArrayToList(T[] arr) {
        if (arr == null || arr.length == 0) {
            return null;
        }
        ArrayList<T> res = new ArrayList<>();
        for (T child : arr) {
            if (child != null) {
                res.add(child);
            }
        }
        return res;
    }
}
