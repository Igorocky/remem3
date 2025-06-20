package org.igye.remem3.app.manager.language.impl;

import org.igye.remem3.app.db.RememDbSchema;
import org.igye.remem3.app.db.entities.LangEnt;
import org.igye.remem3.app.manager.language.LangManager;
import org.igye.remem3.app.manager.language.LangState;
import org.igye.remem3.utils.sqlite.Database;

import java.util.List;

import static org.igye.remem3.app.db.impl.RememDbSchemaImpl.LANG_NAME;

public class LangManagerImpl implements LangManager {
    private final Database db;
    private final RememDbSchema sc;
    private final LangState state;

    public LangManagerImpl(Database db, RememDbSchema sc) {
        this.db = db;
        this.sc = sc;
        state = LangState.builder()
            .allLangs(db.select(LangEnt.class, null, List.of(LANG_NAME), null))
            .build();
    }

    @Override
    public LangState getState() {
        return state;
    }
}
