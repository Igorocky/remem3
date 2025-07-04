package org.igye.remem3.app.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class HistRec {
    private Instant time;
    private String taskType;
    private Double mark;
    private String notes;
}
