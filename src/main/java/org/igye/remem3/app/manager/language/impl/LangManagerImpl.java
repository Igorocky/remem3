package org.igye.remem3.app.manager.language.impl;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.igye.remem3.app.db.entities.LangEnt;
import org.igye.remem3.app.manager.language.LangManager;
import org.igye.remem3.app.manager.language.LangState;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Func;
import org.igye.remem3.utils.sqlite.Database;

import java.util.List;
import java.util.UUID;

import static org.igye.remem3.app.db.impl.RememDbSchemaImpl.LANG_NAME;

@Slf4j
public class LangManagerImpl implements LangManager {
    private final Database db;
    private LangState state;

    public LangManagerImpl(Database db) {
        this.db = db;
        loadStateFromDb();
    }

    @Override
    public LangState getState() {
        return state;
    }

    @Override
    public LangState saveNewLang(String stateId, String newLangName) {
        checkStateId(stateId);
        db.insert(LangEnt.builder().name(newLangName).build());
        loadStateFromDb();
        return getState();
    }

    @Override
    public LangState startEditing(String stateId, Long langId) {
        checkStateId(stateId);
        updateState(st -> st.withEditLangId(langId));
        return getState();
    }

    @Override
    public LangState completeEditing(String stateId, String newLangName) {
        checkStateId(stateId);
        try {
            LangState st = getState();
            Long editLangId = st.getEditLangId();
            st.getAllLangs().stream()
                .filter(lang -> editLangId.equals(lang.id))
                .findFirst()
                .ifPresent(lang -> {
                    lang.name = newLangName;
                    db.update(lang);
                });
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            loadStateFromDb();
        }
        return getState();
    }

    @Override
    public LangState cancelEditing(String stateId) {
        checkStateId(stateId);
        loadStateFromDb();
        return getState();
    }

    private void loadStateFromDb() {
        state = LangState.builder()
            .allLangs(db.select(LangEnt.class, null, List.of(LANG_NAME), null))
            .build();
    }

    @SneakyThrows
    private void updateState(Func<LangState, LangState> update) {
        state = update.apply(state).withId(UUID.randomUUID().toString());
    }

    private void checkStateId(String stateId) {
        if (!state.getId().equals(stateId)) {
            throw new Exn("!state.getId().equals(stateId)");
        }
    }
}
