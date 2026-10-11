package org.igye.remem3.app.controllers.deletecard;

import lombok.Builder;
import lombok.Getter;
import lombok.With;
import org.igye.remem3.app.dto.Card;

import java.util.List;
import java.util.function.Supplier;

@Builder
@Getter
public class State {
    private Card card;
    private Supplier<Object> onComplete;
    private Object onCancel;

    @Builder.Default
    @With
    private List<String> errors = List.of();
}
