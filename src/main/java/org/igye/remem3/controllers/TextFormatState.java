package org.igye.remem3.controllers;

import lombok.Builder;
import lombok.Data;

import java.util.Optional;

@Data
@Builder
public class TextFormatState {
    private String textToFormat;
    private Optional<String> formattedText;
}
