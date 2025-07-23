package org.igye.remem3.app.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.util.List;

@Builder
@Getter
public class BucketDelaysDto {
    private String name;
    private List<Duration> delays;
    private String delaysFromProps;
}
