package org.igye.remem3.app.controllers.movecardstodir;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.web.RequestParams;

import java.io.File;
import java.nio.file.Files;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static org.igye.remem3.app.controllers.movecardstodir.MoveCardsToDirRenderer.ACT_MOVE_SELECTED_BUNDLES;

@RequiredArgsConstructor
public class MoveCardsToDirUpdater implements StateUpdater<State> {
    private final MoveCardsToDirConstructor constructor;

    @Override
    public State update(State st, RequestParams params) {
        if (!st.getErrors().isEmpty()) {
            return st;
        }
        st = constructor.readStateFromParams(params);
        if (params.hasParam(ACT_MOVE_SELECTED_BUNDLES)) {
            actMoveSelectedCards(st);
            return constructor.readStateFromParams(params);
        }
        return st;
    }

    @SneakyThrows
    private void actMoveSelectedCards(State st) {
        for (Bundle bundle : st.getSortedBundlesToList()) {
            if (st.getSelectedBundleIds().contains(bundle.getId())) {
                for (Card card : bundle.getCards()) {
                    File cardFile = card.getFile().get();
                    Files.move(
                        cardFile.toPath(),
                        new File(st.getDirMoveTo().getSelectedDirectory(), cardFile.getName()).toPath(),
                        ATOMIC_MOVE
                    );
                }
            }
        }
    }
}
