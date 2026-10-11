package org.igye.remem3.app.components.impl;

import org.igye.remem3.app.components.PrioritySelectorCmp;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.web.RequestParams;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PrioritySelectorCmpImpl extends HtmlBuilder implements PrioritySelectorCmp {
    public static final int MIN_PRIORITY = 0;
    public static final int MAX_PRIORITY = 3;

    private final List<String> paramNames;
    private Set<Integer> selectedPriorities;

    public PrioritySelectorCmpImpl(String baseParamName) {
        paramNames = new ArrayList<>();
        for (int i = MIN_PRIORITY; i <= MAX_PRIORITY; i++) {
            paramNames.add(baseParamName + "_P" + i);
        }
        this.selectedPriorities = Set.of();
    }

    public PrioritySelectorCmpImpl setSelectedPriorities(RequestParams params) {
        selectedPriorities = new HashSet<>();
        for (int i = MIN_PRIORITY; i <= MAX_PRIORITY; i++) {
            if (params.hasParam(paramNames.get(i - MIN_PRIORITY))) {
                selectedPriorities.add(i);
            }
        }
        return this;
    }

    @Override
    public Set<Integer> getSelectedPriorities() {
        return selectedPriorities;
    }

    @Override
    public HtmlElem render() {
        List<HtmlElem> elems = new ArrayList<>();
        for (int i = MIN_PRIORITY; i <= MAX_PRIORITY; i++) {
            elems.add(text("P" + i));
            String name = paramNames.get(i - MIN_PRIORITY);
            elems.add(
                inpCheckbox(name, name, selectedPriorities.contains(i))
                    .submitOnChange()
                    .attr("style", "margin-left:-6px")
            );
        }
        return frag(elems);
    }
}
