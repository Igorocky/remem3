package org.igye.remem3.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.web.StatefulWebController;

import java.util.Map;
import java.util.Optional;

public class TextFormatController extends HtmlBuilder implements StatefulWebController<TextFormatState, Void> {

    public static final String ATTR_TEXT_TO_FORMAT = "TEXT_TO_FORMAT";

    @Override
    public String getPath() {
        return "format_text";
    }

    @Override
    public TextFormatState loadState(HttpServletRequest req) {
        String textToFormat = Optional.ofNullable(req.getParameter(ATTR_TEXT_TO_FORMAT))
            .map(String::trim)
            .orElse("");
        return TextFormatState.builder()
            .textToFormat(textToFormat)
            .formattedText(
                StringUtils.isBlank(textToFormat)
                    ? Optional.empty()
                    : Optional.of(StringUtils.join(textToFormat.split("\\s+"), "\n"))
            )
            .build();
    }

    @Override
    public Optional<Void> decodeAction(HttpServletRequest req, TextFormatState state) {
        return Optional.empty();
    }

    @Override
    public TextFormatState updateState(TextFormatState state, Void action) {
        return state;
    }

    @Override
    public void saveState(TextFormatState state) {

    }

    @Override
    public String renderState(TextFormatState state) {
        return simplePageWithTitle("Format Text", frag(
            h("form", Map.of("method", "post"),
                h("h4", text("Text to format")),
                h("textarea", Map.of("name", ATTR_TEXT_TO_FORMAT, "cols", "100", "rows", "10"),
                    text(state.getTextToFormat())
                ),
                h("br"),
                h("button", Map.of("type", "submit"), text("Format"))
            ),
            state.getFormattedText().map(formattedText -> frag(
                h("h4", text("Formatted text")),
                h("pre", text(formattedText))
            )).orElse(null)
        )).toString();
    }
}
