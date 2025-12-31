package org.igye.remem3.tools.subtiles;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Duration;
import java.util.List;

@Getter
@Builder
@EqualsAndHashCode
@ToString
public class Subtitle {
    private long idx;
    private Duration start;
    private Duration end;
    private List<String> text;
}
