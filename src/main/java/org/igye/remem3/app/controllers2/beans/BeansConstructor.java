package org.igye.remem3.app.controllers2.beans;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.state.StateConstructor;

@RequiredArgsConstructor
public class BeansConstructor implements StateConstructor<BeansState> {
    public static final String BEANS = "beans";
    private final Settings settings;

    @Override
    public String getName() {
        return BEANS;
    }

    @Override
    public BeansState construct() {
        CustomApplicationContext ctx = new CustomApplicationContext(new CustomBeanExpressionResolver());
        ctx.register(CustomBeansConfig.class);
        ctx.refresh();
        return new BeansState(ctx);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
