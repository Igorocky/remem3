package org.igye.remem3.controllers.validatecards;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;

import java.util.List;

@Builder
@Getter
@With
@EqualsAndHashCode
@ToString
public class ValidateCardsState {
    private List<String> errors;
    private String dir;
}
