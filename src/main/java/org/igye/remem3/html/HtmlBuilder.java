package org.igye.remem3.html;

import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HtmlBuilder {
    @Setter
    private String contextPath;

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
                h("title", text(title)),
                h("script", Map.of("type", "text/javascript", "src", contextPath + "/remem-utils.js"), text(""))
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

    protected HtmlTag inpText(String name, String value, String onEnterBtnId, boolean autofocus) {
        String btnId = StringUtils.isBlank(onEnterBtnId) ? "null" : String.format("\"%s\"", onEnterBtnId);
        return h("input", Map.of(
            "type", "text",
            "name", name,
            "value", value,
            "autocomplete", "off",
            "onkeydown", String.format("preventDefaultOnEnterAction(event,%s)", btnId),
            "autofocus", String.valueOf(autofocus)
        ));
    }

    protected HtmlTag inpText(String name, String value, String onEnterBtnId) {
        return inpText(name, value, onEnterBtnId, false);
    }

    protected HtmlTag inpSubmit(String name, String value) {
        return h("input", Map.of("type", "submit", "id", name, "name", name, "value", value));
    }

    protected HtmlTag table(List<List<? extends HtmlElem>> tableData) {
        return h("table",
            tableData.stream()
                .map(rowData -> h("tr",
                    rowData.stream()
                        .map(cellData -> h("td", cellData))
                        .toList()
                ))
                .toList()
        );
    }

    protected String submitIdParam(String paramName, long id) {
        return paramName + ":" + id;
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
