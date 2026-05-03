package org.igye.remem3.app.controllers2.index;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
public class IndexState {
    private final List<String> allConstructorNames;
}
