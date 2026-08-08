package org.igye.remem3.app.components.impl;

import org.igye.remem3.app.components.PrioritySelectorCmp;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.web.RequestParams;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PrioritySelectorCmpImpl extends HtmlBuilder implements PrioritySelectorCmp {
    private static int NUM_OF_PRIORITIES = 3;

    private final List<String> paramNames;
    private Set<Integer> selectedPriorities;

    public PrioritySelectorCmpImpl(String baseParamName) {
        paramNames = new ArrayList<>();
        for (int i = 0; i < NUM_OF_PRIORITIES; i++) {
            paramNames.add(baseParamName + "_P" + i);
        }
        this.selectedPriorities = Set.of();
    }

    public void setSelectedPriorities(RequestParams params) {
        selectedPriorities = new HashSet<>();
        for (int i = 0; i < NUM_OF_PRIORITIES; i++) {
            if (params.hasParam(paramNames.get(i))) {
                selectedPriorities.add(i);
            }
        }
    }

    @Override
    public Set<Integer> getSelectedPriorities() {
        return selectedPriorities.stream().map(i -> i + 1).collect(Collectors.toSet());
    }

    @Override
    public HtmlElem render() {
        List<HtmlElem> elems = new ArrayList<>();
        for (int i = 0; i < NUM_OF_PRIORITIES; i++) {
            elems.add(text("P" + (i + 1)));
            String name = paramNames.get(i);
            elems.add(inpCheckbox(name, name, selectedPriorities.contains(i)).submitOnChange());
        }
        return frag(elems);
    }
}
