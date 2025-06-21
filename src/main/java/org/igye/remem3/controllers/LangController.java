package org.igye.remem3.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.igye.remem3.app.db.entities.LangEnt;
import org.igye.remem3.app.manager.language.LangManager;
import org.igye.remem3.app.manager.language.LangState;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.utils.web.impl.RequestParamsImpl;
import org.igye.remem3.web.StatefulWebController;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

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
        RequestParamsImpl params = new RequestParamsImpl(req);
        String stateId = params.getParam(PAR_STATE_ID);
        if (params.hasParam(ACT_SAVE_NEW_LANG)) {
            return Optional.of(() -> langManager.saveNewLang(
                stateId,
                params.getParam(PAR_NEW_LANG_NAME)
            ));
        }
        if (params.hasSubmitIdParam(ACT_START_EDITING_LANG)) {
            return Optional.of(() -> langManager.startEditing(
                stateId,
                params.getSubmitIdParamLong(ACT_START_EDITING_LANG)
            ));
        }
        if (params.hasParam(ACT_SAVE_EDITED_LANG)) {
            return Optional.of(() -> langManager.completeEditing(
                stateId,
                params.getParam(PAR_EDITED_LANG_NAME)
            ));
        }
        if (params.hasParam(ACT_DISCARD_EDITED_LANG)) {
            return Optional.of(() -> langManager.cancelEditing(stateId));
        }
        return Optional.empty();
    }

    @Override
    public synchronized LangState updateState(LangState state, Supplier<LangState> action) {
        return action.get();
    }

    @Override
    public void saveState(LangState state) {

    }

    @Override
    public String renderState(LangState state) {
        return simplePageWithTitle("Languages",
            form(
                inpHidden(PAR_STATE_ID, state.getId()),
                h4(text("Languages")),
                rndLangs(state.getAllLangs(), state.getEditLangId())
            )
        ).toString();
    }

    private HtmlElem rndLangs(List<LangEnt> langs, Long editLangId) {
        return frag(
            table(
                langs.stream().map(lang -> {
                    if (lang.id.equals(editLangId)) {
                        return List.of(
                            inpText(PAR_EDITED_LANG_NAME, lang.name, ACT_SAVE_EDITED_LANG, true),
                            inpSubmit(ACT_SAVE_EDITED_LANG, "Save"),
                            inpSubmit(ACT_DISCARD_EDITED_LANG, "Cancel")
                        );
                    } else {
                        return List.of(
                            text(lang.name),
                            inpSubmit(submitIdParam(ACT_START_EDITING_LANG, lang.id), "Edit")
                        );
                    }
                }).toList()
            ),
            editLangId != null ? null : table(List.of(List.of(
                inpText(PAR_NEW_LANG_NAME, "", ACT_SAVE_NEW_LANG, true),
                inpSubmit(ACT_SAVE_NEW_LANG, "Add new language")
            )))
        );
    }
}
