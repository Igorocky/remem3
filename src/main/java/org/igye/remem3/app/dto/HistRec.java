package org.igye.remem3.app.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class HistRec implements org.igye.remem3.app.repeatstrategy.HistRec {
    private Instant time;
    private RepeatStrategyType strategy;
    private String taskType;
    private BigDecimal mark;
    private String notes;
}
