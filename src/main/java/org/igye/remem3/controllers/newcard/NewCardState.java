package org.igye.remem3.controllers.newcard;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import org.igye.remem3.app.dto.Card;

@Builder
@Getter
@With
@EqualsAndHashCode
@ToString
public class NewCardState {
    private String dir;
    private Card card;
}
