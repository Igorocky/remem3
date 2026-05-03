package org.igye.remem3.test.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlFragment;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.html.HtmlText;
import org.igye.remem3.test.TestUtils;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.web.RequestParams;
import org.igye.remem3.web.impl.RequestParamsImpl;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestUtilsImpl implements TestUtils {

    private static final HtmlTag[] HTML_TAGS_EMPTY_ARR = {};

    @Override
    public void assertInputs(HtmlElem html, HtmlTag... elems) {
        ArrayList<HtmlTag> expectedInputs = new ArrayList<>(Arrays.asList(elems));
        ArrayList<HtmlTag> actualInputs = new ArrayList<>(getAllInputs(html).toList());
        for (int eIdx = 0; eIdx < expectedInputs.size(); eIdx++) {
            HtmlTag expected = expectedInputs.get(eIdx);
            for (int aIdx = 0; aIdx < actualInputs.size(); aIdx++) {
                HtmlTag actual = actualInputs.get(aIdx);
                if (equals(expected, actual)) {
                    expectedInputs.remove(eIdx--);
                    actualInputs.remove(aIdx);
                    break;
                }
            }
        }
        for (int aIdx = 0; aIdx < actualInputs.size(); aIdx++) {
            HtmlTag actual = actualInputs.get(aIdx);
            for (int eIdx = 0; eIdx < expectedInputs.size(); eIdx++) {
                HtmlTag expected = expectedInputs.get(eIdx);
                if (equals(actual, expected)) {
                    actualInputs.remove(aIdx--);
                    expectedInputs.remove(eIdx);
                    break;
                }
            }
        }
        if (CollectionUtils.isNotEmpty(expectedInputs) || CollectionUtils.isNotEmpty(actualInputs)) {
            String expectedTagsStr = expectedInputs.stream()
                .map(HtmlTag::toString)
                .collect(Collectors.joining("\n"));
            String actualTagsStr = actualInputs.stream()
                .map(HtmlTag::toString)
                .collect(Collectors.joining("\n"));
            throw new Exn(String.format(
                "\nExpected(%s):\n%s\nUnexpected(%s):\n%s",
                expectedInputs.size(),
                expectedTagsStr,
                actualInputs.size(),
                actualTagsStr
            ));
        }
    }

    @Override
    public void assertInputs(HtmlElem html, List<HtmlTag> elems) {
        assertInputs(html, elems.toArray(HTML_TAGS_EMPTY_ARR));
    }

    @Override
    public void setValue(HtmlElem html, String paramName, String value) {
        List<HtmlTag> inputs = getAllWritableInputsByParamName(html, paramName);
        if (inputs.size() != 1) {
            throw new Exn("No active input found for name '%s'".formatted(paramName));
        }
        inputs.getFirst().attr("value", value);
    }

    @Override
    public RequestParams submit(HtmlElem html, String submitButtonName) {
        List<HtmlTag> forms = getAllForms(html).toList();
        if (forms.size() > 1) {
            throw new Exn("forms.size() > 1");
        }
        Map<String, List<String>> reqParams = new HashMap<>();
        getAllInputs(forms.isEmpty() ? html : forms.getFirst())
            .filter(inp -> !isDisabled(inp))
            .forEach(inp -> {
                if (isInpSubmit(inp)) {
                    if (submitButtonName.equals(getName(inp))) {
                        reqParams.computeIfAbsent(submitButtonName, _ -> new ArrayList<>()).add(getValue(inp));
                    }
                } else if (isInpCheckbox(inp) && isChecked(inp)) {
                    reqParams.computeIfAbsent(getName(inp), _ -> new ArrayList<>()).add(getValue(inp));
                } else if (isSelect(inp)) {
                    CollectionUtils.emptyIfNull(inp.getChildren()).stream()
                        .filter(ch -> ch instanceof HtmlTag)
                        .map(ch -> (HtmlTag) ch)
                        .filter(opt -> "option".equals(opt.getName()) && isSelected(opt))
                        .forEach(opt -> reqParams.computeIfAbsent(getName(inp), _ -> new ArrayList<>()).add(getValue(opt)));
                } else {
                    reqParams.computeIfAbsent(getName(inp), _ -> new ArrayList<>()).add(getValue(inp));
                }
            });
        String[] emptyStringArr = {};
        Map<String, String[]> parameterMap = reqParams.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().toArray(emptyStringArr)
            ));
        HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(httpServletRequest.getParameterMap()).thenReturn(parameterMap);
        return new RequestParamsImpl(httpServletRequest);
    }

    @Override
    public boolean isWritable(HtmlElem html, String name) {
        return getAllWritableInputsByParamName(html, name).stream().anyMatch(tag -> name.equals(getName(tag)));
    }

    private List<HtmlTag> getAllWritableInputsByParamName(HtmlElem html, String paramName) {
        return getAllInputs(html)
            .filter(inp -> paramName.equals(getName(inp)) && !isHidden(inp) && !isDisabled(inp))
            .toList();
    }

    private boolean equals(HtmlElem a, HtmlElem b) {
        return switch (a) {
            case HtmlFragment aFrag -> switch (b) {
                case HtmlFragment bFrag -> equals(aFrag.getChildren(), bFrag.getChildren());
                case HtmlTag _, HtmlText _ -> false;
            };
            case HtmlTag aTag -> switch (b) {
                case HtmlTag bTag -> Objects.equals(aTag.getName(), bTag.getName())
                    && Objects.equals(aTag.getAttrs(), bTag.getAttrs())
                    && equals(aTag.getChildren(), bTag.getChildren());
                case HtmlFragment _, HtmlText _ -> false;
            };
            case HtmlText aText -> switch (b) {
                case HtmlText bText -> Objects.equals(aText.getText(), bText.getText());
                case HtmlFragment _, HtmlTag _ -> false;
            };
        };
    }

    private boolean equals(List<? extends HtmlElem> children1, List<? extends HtmlElem> children2) {
        if (CollectionUtils.isEmpty(children1)) {
            return CollectionUtils.isEmpty(children2);
        } else if (CollectionUtils.isEmpty(children2)) {
            return false;
        } else {
            for (int i = 0; i < children1.size(); i++) {
                if (!equals(children1.get(i), children2.get(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    private Stream<HtmlTag> getAllTags(HtmlElem html, Predicate<HtmlTag> predicate) {
        return switch (html) {
            case HtmlText _ -> Stream.empty();
            case HtmlFragment frag -> getAllTags(frag.getChildren(), predicate);
            case HtmlTag tag -> {
                if (predicate.test(tag)) {
                    yield Stream.of(tag);
                } else {
                    yield getAllTags(tag.getChildren(), predicate);
                }
            }
        };
    }

    private Stream<HtmlTag> getAllTags(List<? extends HtmlElem> children, Predicate<HtmlTag> predicate) {
        if (CollectionUtils.isEmpty(children)) {
            return Stream.empty();
        } else {
            return children.stream().flatMap(ch -> getAllTags(ch, predicate));
        }
    }

    private Stream<HtmlTag> getAllInputs(HtmlElem html) {
        return getAllTags(html, tag -> {
            String name = tag.getName();
            return "input".equals(name) || "textarea".equals(name) || "select".equals(name);
        });
    }

    private Stream<HtmlTag> getAllForms(HtmlElem html) {
        return getAllTags(html, tag -> "form".equals(tag.getName()));
    }

    private boolean isInpSubmit(HtmlTag inp) {
        return "submit".equals(getType(inp));
    }

    private boolean isInpCheckbox(HtmlTag inp) {
        return "checkbox".equals(getType(inp));
    }

    private boolean isSelect(HtmlTag inp) {
        return "select".equals(getType(inp));
    }

    private boolean isHidden(HtmlTag inp) {
        return "hidden".equals(getType(inp));
    }

    private String getAttr(HtmlTag tag, String attrName) {
        Map<String, String> attrs = tag.getAttrs();
        if (attrs == null) {
            return null;
        } else {
            return attrs.get(attrName);
        }
    }

    private boolean hasAttr(HtmlTag tag, String attrName) {
        Map<String, String> attrs = tag.getAttrs();
        if (attrs == null) {
            return false;
        } else {
            return attrs.containsKey(attrName);
        }
    }

    private String getType(HtmlTag tag) {
        return getAttr(tag, "type");
    }

    private String getName(HtmlTag tag) {
        return getAttr(tag, "name");
    }

    private String getValue(HtmlTag tag) {
        return getAttr(tag, "value");
    }

    private boolean isChecked(HtmlTag tag) {
        return hasAttr(tag, "checked");
    }

    private boolean isSelected(HtmlTag tag) {
        return hasAttr(tag, "selected");
    }

    private boolean isDisabled(HtmlTag tag) {
        return hasAttr(tag, "disabled");
    }
}
