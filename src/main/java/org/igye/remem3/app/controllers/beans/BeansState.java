package org.igye.remem3.app.controllers.beans;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Shell;

import java.util.List;

@RequiredArgsConstructor
@Getter
public class BeansState {
    private final Shell sh;

    public List<Pair<String, Object>> getBeans() {
        return sh.getBeans();
    }

    public <T> List<Pair<String, T>> getBeans(Class<T> type) {
        return sh.getBeans(type);
    }
}
