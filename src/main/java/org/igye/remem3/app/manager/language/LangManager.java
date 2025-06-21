package org.igye.remem3.app.manager.language;

public interface LangManager {
    LangState getState();

    LangState saveNewLang(String stateId, String newLangName);

    LangState startEditing(String stateId, Long langId);

    LangState completeEditing(String stateId, String newLangName);

    LangState cancelEditing(String stateId);

    LangState startDeleting(String stateId, Long langId);

    LangState completeDeleting(String stateId);

    LangState cancelDeleting(String stateId);
}
