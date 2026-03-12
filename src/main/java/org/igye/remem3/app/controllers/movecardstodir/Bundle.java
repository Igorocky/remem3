package org.igye.remem3.app.controllers.movecardstodir;

import lombok.Builder;
import lombok.Getter;
import org.igye.remem3.app.dto.Card;

import java.util.List;

@Builder
@Getter
public class Bundle {
    private String id;
    private List<Card> cards;
    private long rating;
}
