package org.igye.remem3.app.controllers.deletecard;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer;
import org.igye.remem3.app.state.StateRenderer;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class DeleteCardRenderer extends HtmlBuilder implements StateRenderer<State> {
    public static final String ACT_DELETE = "DeleteCard_ACT_DELETE";
    public static final String ACT_CANCEL = "DeleteCard_ACT_CANCEL";

    private final CardExplorerRenderer cardExplorerRenderer;

    @Override
    public HtmlElem render(State st) {
        return simplePageWithTitle("Delete card",
            h4(text("Delete this card?")),
            rndErrors(st.getErrors()),
            form(
                span("color:lightgrey;", text(st.getCard().getFile().get().getAbsolutePath())),
                cardExplorerRenderer.rndCard(st.getCard(), false, Optional.empty()),
                rndButtons()
            )
        );
    }

    private HtmlElem rndButtons() {
        return table(List.of(List.of(
            inpSubmit(ACT_DELETE, "Delete"),
            inpSubmit(ACT_CANCEL, "Cancel")
        )));
    }
}
