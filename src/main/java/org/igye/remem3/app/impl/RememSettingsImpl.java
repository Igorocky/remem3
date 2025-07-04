package org.igye.remem3.app.impl;

import lombok.Builder;
import lombok.Data;
import org.igye.remem3.app.RememSettings;

import java.util.Collections;
import java.util.List;

@Data
@Builder
public class RememSettingsImpl implements RememSettings {
    @Builder.Default
    private List<String> languages = Collections.emptyList();
}
