package org.igye.remem3.html;

import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class HtmlFragment implements HtmlElem {
    private final List<? extends HtmlElem> children;

    public HtmlFragment(List<? extends HtmlElem> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        if (!CollectionUtils.isEmpty(children)) {
            return children.stream()
                .filter(Objects::nonNull)
                .map(HtmlElem::toString)
                .collect(Collectors.joining("\n"));
        } else {
            return "";
        }
    }
}
