package org.igye.remem3.app.controllers.makenewdir;

import org.apache.commons.collections4.CollectionUtils;
import org.igye.remem3.app.components.DirSelectorCmp;
import org.igye.remem3.app.state.StateRenderer;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;

import java.util.List;

public class MakeNewDirRenderer extends HtmlBuilder implements StateRenderer<State> {
    public static final String PAR_PARENT_DIR = "MakeNewDir_PAR_PARENT_DIR";
    public static final String PAR_NEW_DIR_NAME = "MakeNewDir_PAR_NEW_DIR_NAME";
    public static final String ACT_CREATE = "MakeNewDir_ACT_CREATE";
    public static final String ACT_CANCEL = "MakeNewDir_ACT_CANCEL";

    @Override
    public HtmlElem render(State st) {
        return simplePageWithTitle("Create new directory",
            h4(text("Create new directory")),
            rndErrors(st.getErrors()),
            form(
                rndParentDir(st.getParentDir()),
                rndNewDirName(st.getNewDirName()),
                rndButtons()
            )
        );
    }

    private HtmlElem rndButtons() {
        return table(List.of(List.of(
            inpSubmit(ACT_CREATE, "Create"),
            inpSubmit(ACT_CANCEL, "Cancel")
        )));
    }

    private HtmlElem rndNewDirName(String newDirName) {
        return table(List.of(List.of(
            text("New directory name"),
            inpText(PAR_NEW_DIR_NAME, newDirName, ACT_CREATE).autofocus().attr("size", "100")
        )));
    }

    private HtmlElem rndParentDir(DirSelectorCmp parentDir) {
        return table(List.of(List.of(
            text("Parent directory"),
            parentDir.render()
        )));
    }


    private HtmlElem rndErrors(List<String> errors) {
        if (CollectionUtils.isEmpty(errors)) {
            return null;
        }
        return div("color:red;",
            h3(text("Error")),
            ul(errors.stream().map(msg -> pre(text(msg))).toList())
        );
    }
}
