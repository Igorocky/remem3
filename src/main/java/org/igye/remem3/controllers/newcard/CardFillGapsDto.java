package org.igye.remem3.controllers.newcard;

import lombok.Builder;
import lombok.Getter;
import lombok.With;
import org.igye.remem3.app.dto.CardType;

import static org.igye.remem3.app.dto.CardType.FILL_GAPS;

@Builder
@Getter
@With
public final class CardFillGapsDto implements CardDto {
    private String lang;
    private String text;
    private String notes;

    @Override
    public CardType getType() {
        return FILL_GAPS;
    }
}