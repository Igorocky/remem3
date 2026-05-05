package org.igye.remem3.app.controllers2.beans;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.state.StateConstructor;
import org.springframework.context.support.FileSystemXmlApplicationContext;

@RequiredArgsConstructor
public class BeansConstructor implements StateConstructor<BeansState> {
    private final Settings settings;

    @Override
    public String getName() {
        return "beans";
    }

    @Override
    public BeansState construct() {
        return new BeansState(new FileSystemXmlApplicationContext(settings.getBeansFile()));
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
