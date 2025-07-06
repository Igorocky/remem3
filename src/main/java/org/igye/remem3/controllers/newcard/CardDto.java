package org.igye.remem3.controllers.newcard;

import org.igye.remem3.app.dto.CardType;

public sealed interface CardDto permits CardFillGapsDto {
    CardType getType();
}