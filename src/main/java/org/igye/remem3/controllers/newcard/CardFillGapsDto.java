package org.igye.remem3.controllers.newcard;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import static org.igye.remem3.controllers.newcard.NewCardController.CARD_TYPE_FILL_GAPS;

@Builder
@Getter
@With
public class CardFillGapsDto implements CardDto {
    private String lang;
    private String text;
    private String notes;

    @Override
    public String getCardType() {
        return CARD_TYPE_FILL_GAPS;
    }
}