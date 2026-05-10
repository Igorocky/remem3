package org.igye.remem3.app.controllers.index;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.state.StateConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
public class IndexState {
    private final List<StateConstructor> stateConstructors;
}
