package org.igye.remem3.app.controllers.deletecard;

import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.web.RequestParams;

import java.nio.file.Files;
import java.util.List;

import static org.igye.remem3.app.controllers.deletecard.DeleteCardRenderer.ACT_CANCEL;
import static org.igye.remem3.app.controllers.deletecard.DeleteCardRenderer.ACT_DELETE;

public class DeleteCardUpdater implements StateUpdater<State> {

    @Override
    public Object update(State st, RequestParams params) {
        st = st.withErrors(List.of());
        if (params.hasParam(ACT_DELETE)) {
            try {
                Files.delete(st.getCard().getFile().get().toPath());
                return st.getOnComplete().get();
            } catch (Exception ex) {
                st = st.withErrors(List.of(ex.toString()));
                return st;
            }
        } else if (params.hasParam(ACT_CANCEL)) {
            return st.getOnCancel();
        }
        return st;
    }
}
