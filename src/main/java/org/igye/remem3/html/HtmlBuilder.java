package org.igye.remem3.html;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HtmlBuilder {
    protected static final String GREEN = "#0077008a";
    protected static final String RED = "#ff00008f";
    protected static final String ORANGE = "#ffa500a3";

    protected HtmlText text(String text) {
        return new HtmlText(text);
    }

    protected HtmlText text(Object obj) {
        return new HtmlText(String.valueOf(obj));
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

    protected HtmlTag a(String url, HtmlElem content) {
        return h("a", Map.of("href", url), content);
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
                h("meta", Map.of("charset", "UTF-8")),
                h("title", text(title)),
                h("script", Map.of("type", "text/javascript", "src", "/remem-utils.js"), text("")),
                h("link", Map.of("rel", "stylesheet", "href", "/remem.css"))
            ),
            h("body", children)
        );
    }

    protected HtmlTag simplePageWithTitle(String title, HtmlElem... children) {
        return simplePageWithTitle(title, childrenArrayToList(children));
    }

    protected HtmlTag h1(List<? extends HtmlElem> content) {
        return header("h1", content);
    }

    protected HtmlTag h1(HtmlElem... content) {
        return h1(childrenArrayToList(content));
    }

    protected HtmlTag h2(List<? extends HtmlElem> content) {
        return header("h2", content);
    }

    protected HtmlTag h2(HtmlElem... content) {
        return h2(childrenArrayToList(content));
    }

    protected HtmlTag h3(List<? extends HtmlElem> content) {
        return header("h3", content);
    }

    protected HtmlTag h3(HtmlElem... content) {
        return h3(childrenArrayToList(content));
    }

    protected HtmlTag h4(List<? extends HtmlElem> content) {
        return header("h4", content);
    }

    protected HtmlTag h4(HtmlElem... content) {
        return h4(childrenArrayToList(content));
    }

    protected HtmlTag h5(List<? extends HtmlElem> content) {
        return header("h5", content);
    }

    protected HtmlTag h5(HtmlElem... content) {
        return h5(childrenArrayToList(content));
    }

    protected HtmlTag h6(List<? extends HtmlElem> content) {
        return header("h6", content);
    }

    protected HtmlTag h6(HtmlElem... content) {
        return h6(childrenArrayToList(content));
    }

    protected HtmlTag br() {
        return h("br");
    }

    protected HtmlTag hr() {
        return h("hr");
    }

    protected HtmlTag pre(List<? extends HtmlElem> content) {
        return h("pre", content);
    }

    protected HtmlTag pre(HtmlElem... content) {
        return pre(childrenArrayToList(content));
    }

    protected HtmlTag div(String style, List<? extends HtmlElem> children) {
        return h(
            "div",
            style == null ? null : Map.of("style", style),
            CollectionUtils.isEmpty(children) ? List.of(text("")) : children
        );
    }

    protected HtmlTag div(String style, HtmlElem... content) {
        return div(style, childrenArrayToList(content));
    }

    protected HtmlTag div(List<? extends HtmlElem> children) {
        return div(null, children);
    }

    protected HtmlTag div(HtmlElem... content) {
        return div(null, content);
    }

    protected HtmlTag table(List<? extends List<? extends HtmlElem>> tableData) {
        return h("table",
            tableData.stream()
                .map(rowData -> h("tr",
                    rowData.stream()
                        .map(cellData -> h("td", cellData == null ? text("") : cellData))
                        .toList()
                ))
                .toList()
        );
    }

    protected HtmlTag ul(List<? extends HtmlElem> items) {
        return htmlList("ul", items);
    }

    protected HtmlTag ol(List<? extends HtmlElem> items) {
        return htmlList("ol", items);
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

    protected HtmlTag inpText(String name, String value, String onEnterBtnId) {
        String btnId = StringUtils.isBlank(onEnterBtnId) ? "null" : "'%s'".formatted(onEnterBtnId);
        Map<String, String> attrs = new HashMap<>(Map.of(
            "type", "text",
            "name", name,
            "value", value,
            "autocomplete", "off",
            "onkeydown", "preventDefaultOnEnterAction(event,false,%s)".formatted(btnId)
        ));
        return h("input", attrs);
    }

    protected HtmlTag inpSubmit(String name, String value) {
        return h("input", Map.of("type", "submit", "id", name, "name", name, "value", value));
    }

    protected HtmlTag inpCheckbox(String name, String value, boolean checked) {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("type", "checkbox");
        attrs.put("name", name);
        attrs.put("value", value);
        if (checked) {
            attrs.put("checked", "checked");
        }
        return h("input", attrs);
    }

    protected HtmlTag select(
        String name,
        String selected,
        List<? extends Pair<String, ? extends HtmlElem>> options
    ) {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("name", name);
        return h("select", attrs,
            options.stream()
                .map(option -> {
                    Map<String, String> optionAttrs = new HashMap<>();
                    String value = option.getLeft();
                    optionAttrs.put("value", value);
                    if (value.equals(selected)) {
                        optionAttrs.put("selected", "");
                    }
                    return h("option", optionAttrs, option.getRight());
                })
                .toList()
        );
    }

    @SafeVarargs
    protected final HtmlTag select(
        String name,
        String selected,
        Pair<String, ? extends HtmlElem>... options
    ) {
        return select(name, selected, childrenArrayToList(options));
    }

    protected HtmlTag textarea(String name, String value, int cols, int rows) {
        return h("textarea", Map.of("name", name, "cols", String.valueOf(cols), "rows", String.valueOf(rows)),
            text(value)
        );
    }

    protected String keyValueParam(String key, String value) {
        return key + ":" + value;
    }

    protected String keyValueParam(String key, long value) {
        return key + ":" + value;
    }

    private <T> List<T> childrenArrayToList(T[] arr) {
        if (arr == null || arr.length == 0) {
            return null;
        }
        ArrayList<T> res = new ArrayList<>(arr.length);
        for (T child : arr) {
            if (child != null) {
                res.add(child);
            }
        }
        return res;
    }

    private HtmlTag htmlList(String listTag, List<? extends HtmlElem> items) {
        return h(listTag,
            items.stream().map(item -> h("li", item)).toList()
        );
    }

    private HtmlTag header(String tag, List<? extends HtmlElem> children) {
        List<HtmlElem> ch = new ArrayList<>(children);
        //adding empty text for the header to be always with the closing tag
        ch.add(text(""));
        return h(tag, ch);
    }
}
