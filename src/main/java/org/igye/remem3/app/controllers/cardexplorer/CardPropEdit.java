package org.igye.remem3.app.controllers.cardexplorer;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class CardPropEdit {
    private final String cardPath;
    private final EditableProp prop;
    private final String text;
    private final List<String> errors;
}
