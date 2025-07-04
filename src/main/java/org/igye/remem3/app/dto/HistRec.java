package org.igye.remem3.app.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Builder
@Getter
public class HistRec {
    private Instant time;
    private String taskType;
    private Double mark;
    private String notes;
}
