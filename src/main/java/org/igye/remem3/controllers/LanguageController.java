package org.igye.remem3.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.igye.remem3.app.db.entities.LangEnt;
import org.igye.remem3.app.manager.language.LangAction;
import org.igye.remem3.app.manager.language.LangManager;
import org.igye.remem3.app.manager.language.LangState;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.web.StatefulWebController;

import java.util.List;
import java.util.Optional;

public class LanguageController extends HtmlBuilder
    implements StatefulWebController<LangState, LangAction> {
    private static final String PARAM_NEW_LANG_NAME = "new_lang_name";
    private final LangManager langManager;

    public LanguageController(LangManager langManager) {
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
    public Optional<LangAction> decodeAction(HttpServletRequest req, LangState state) {
        return Optional.empty();
    }

    @Override
    public LangState updateState(LangState state, LangAction action) {
        return state;
    }

    @Override
    public void saveState(LangState state) {

    }

    @Override
    public String renderState(LangState state) {
        return simplePageWithTitle(
            "Languages",
            frag()
        ).toString();
    }

    private HtmlElem rndLangs(List<LangEnt> langs, Long editLangId) {
        return null;
//        return h("table", langs.stream().map(lang -> {
//            List<HtmlElem> cells = new ArrayList<>();
//            if (lang.id != null) {
//                if (lang.id.equals(editLangId)) {
//                    cells.add(text("[Editing lang]"));
//                } else {
//                    cells.add(text(lang.name));
//                }
//            } else {
//                cells.add(h("input", Map.of(
//                    "type","text",
//                    "name", PARAM_NEW_LANG_NAME,
//                    "value", ""
//                )));
//                cells.add(h("input", Map.of(
//                    "type","submit",
//                    "name", PARAM_NEW_LANG_NAME,
//                    "value", ""
//                )));
//            }
//            return h("tr");
//        }));
    }
}
