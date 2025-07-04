package org.igye.remem3.app.dto.fillgaps;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.HistRec;

import java.io.File;
import java.util.List;

@Builder
@Getter
@ToString
@EqualsAndHashCode
@With
public class CardFillGaps implements Card {
    private File file;
    private String lang;
    private List<TextPart> text;
    private String notes;
    private List<HistRec> history;
}
