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

    protected HtmlFragment frag(List<? extends HtmlElem> children) {
        return new HtmlFragment(children);
    }

    protected HtmlTag simplePageWithTitle(String title, List<? extends HtmlElem> children) {
        return h("html",
            h("head",
                h("title", text(title))
            ),
            h("body", children)
        );
    }

    protected HtmlTag simplePageWithTitle(String title, HtmlElem... children) {
        return simplePageWithTitle(title, childrenArrayToList(children));
    }

    protected HtmlTag h1(HtmlElem content) {
        return h("h1", content);
    }

    protected HtmlTag h2(HtmlElem content) {
        return h("h2", content);
    }

    protected HtmlTag h3(HtmlElem content) {
        return h("h3", content);
    }

    protected HtmlTag h4(HtmlElem content) {
        return h("h4", content);
    }

    protected HtmlTag h5(HtmlElem content) {
        return h("h5", content);
    }

    protected HtmlTag h6(HtmlElem content) {
        return h("h6", content);
    }

    protected HtmlTag form(List<? extends HtmlElem> children) {
        return h("form", Map.of("method", "post"), children);
    }

    protected HtmlTag form(HtmlElem... children) {
        return form(childrenArrayToList(children));
    }

    protected HtmlTag inpHidden(String name, String value) {
        return h("input", Map.of("type", "hidden", "name", name, "value", value));
    }

    protected HtmlTag inpText(String name, String value) {
        return h("input", Map.of("type", "text", "name", name, "value", value));
    }

    protected HtmlTag inpSubmit(String name, String value) {
        return h("input", Map.of("type", "submit", "name", name, "value", value));
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
