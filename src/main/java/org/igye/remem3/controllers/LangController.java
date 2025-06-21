package org.igye.remem3.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.igye.remem3.app.db.entities.LangEnt;
import org.igye.remem3.app.manager.language.LangManager;
import org.igye.remem3.app.manager.language.LangState;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.web.StatefulWebController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class LangController extends HtmlBuilder
    implements StatefulWebController<LangState, Supplier<LangState>> {

    private static final String PAR_STATE_ID = "PAR_STATE_ID";
    private static final String ACT_START_EDITING_LANG = "ACT_START_EDITING_LANG";
    private static final String PAR_EDITED_LANG_NAME = "PAR_EDITED_LANG_NAME";
    private static final String ACT_SAVE_EDITED_LANG = "ACT_SAVE_EDITED_LANG";
    private static final String ACT_DISCARD_EDITED_LANG = "ACT_DISCARD_EDITED_LANG";
    private static final String PAR_NEW_LANG_NAME = "PAR_NEW_LANG_NAME";
    private static final String ACT_SAVE_NEW_LANG = "ACT_SAVE_NEW_LANG";

    private final LangManager langManager;

    public LangController(LangManager langManager) {
        this.langManager = langManager;
    }

    @Override
    public String getPath() {
        return "languages";
    }

    @Override
    public LangState loadState(HttpServletRequest req) {
        return langManager.getState();
    }

    @Override
    public Optional<Supplier<LangState>> decodeAction(HttpServletRequest req, LangState state) {
        Map<String, String[]> params = req.getParameterMap();
        if (params.containsKey(ACT_SAVE_NEW_LANG)) {
            return Optional.of(() -> langManager.saveNewLang(
                req.getParameter(PAR_STATE_ID),
                req.getParameter(PAR_NEW_LANG_NAME)
            ));
        }
        return Optional.empty();
    }

    @Override
    public LangState updateState(LangState state, Supplier<LangState> action) {
        return action.get();
    }

    @Override
    public void saveState(LangState state) {

    }

    @Override
    public String renderState(LangState state) {
        return simplePageWithTitle(
            "Languages",
            h("form", Map.of("method", "post"),
                h("input", Map.of(
                    "type", "hidden",
                    "name", PAR_STATE_ID,
                    "value", state.getId()
                )),
                h("h4", text("Languages")),
                h("br"),
                rndLangs(state.getAllLangs(), state.getEditLangId())
            )
        ).toString();
    }

    private HtmlElem rndLangs(List<LangEnt> langs, Long editLangId) {
        List<HtmlTag> rows = new ArrayList<>(langs.stream().map(lang -> {
            List<HtmlElem> cells = new ArrayList<>();
            if (lang.id.equals(editLangId)) {
                cells.add(h("input", Map.of(
                    "type", "text",
                    "name", PAR_EDITED_LANG_NAME,
                    "value", lang.name
                )));
                cells.add(h("input", Map.of(
                    "type", "submit",
                    "name", ACT_SAVE_EDITED_LANG,
                    "value", "Save"
                )));
                cells.add(h("input", Map.of(
                    "type", "submit",
                    "name", ACT_DISCARD_EDITED_LANG,
                    "value", "Cancel"
                )));
            } else {
                cells.add(text(lang.name));
                cells.add(h("input", Map.of(
                    "type", "submit",
                    "name", ACT_START_EDITING_LANG,
                    "value", "Edit"
                )));
            }
            return h("tr", cells.stream().map(cell -> h("td", cell)).toList());
        }).toList());
        if (editLangId == null) {
            rows.add(h("tr", Stream.of(
                h("input", Map.of(
                    "type", "text",
                    "name", PAR_NEW_LANG_NAME,
                    "value", ""
                )),
                h("input", Map.of(
                    "type", "submit",
                    "name", ACT_SAVE_NEW_LANG,
                    "value", "Add new language"
                ))
            ).map(cell -> h("td", cell)).toList()));
        }
        return h("table", rows);
    }
}
