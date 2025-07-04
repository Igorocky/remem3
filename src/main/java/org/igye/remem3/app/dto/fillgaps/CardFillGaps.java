package org.igye.remem3.app.dto.fillgaps;

import lombok.Builder;
import lombok.Getter;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.HistRec;

import java.util.List;

@Builder
@Getter
public class CardFillGaps implements Card {
    private String lang;
    private List<TextPart> text;
    private String notes;
    private List<HistRec> history;
}
