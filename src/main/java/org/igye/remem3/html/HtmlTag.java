package org.igye.remem3.html;

import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class HtmlTag implements HtmlElem {
    @Getter
    private final String name;
    @Getter
    private Map<String, String> attrs;
    @Getter
    private final List<? extends HtmlElem> children;

    public HtmlTag(String name, Map<String, String> attrs, List<? extends HtmlElem> children) {
        this.name = name;
        this.attrs = attrs;
        this.children = children;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(name);
        if (attrs != null && !attrs.isEmpty()) {
            attrs.forEach((attrName, attrValue) ->
                sb.append(" ")
                    .append(attrName)
                    .append("=\"")
                    .append(StringEscapeUtils.escapeHtml4(attrValue))
                    .append("\"")
            );
        }
        if (CollectionUtils.isEmpty(children)) {
            sb.append("/>");
        } else {
            sb.append(">\n").append(
                children.stream()
                    .filter(Objects::nonNull)
                    .map(HtmlElem::toString)
                    .collect(Collectors.joining("\n"))
            );
            sb.append("\n</").append(name).append(">");
        }
        return sb.toString();
    }

    public HtmlTag attr(String attrName, String attrValue) {
        if (!(attrs instanceof HashMap<String, String>)) {
            attrs = attrs == null ? new HashMap<>() : new HashMap<>(attrs);
        }
        attrs.put(attrName, attrValue);
        return this;
    }

    public HtmlTag disabled() {
        return attr("disabled", "");
    }

    public HtmlTag autofocus() {
        return attr("autofocus", "");
    }

    public HtmlTag submitOnChange() {
        return attr("onchange", "this.form.submit()");
    }

    public HtmlTag id(String id) {
        return attr("id", id);
    }

    public HtmlTag onkeydown(String callback) {
        return attr("onkeydown", callback);
    }
}
