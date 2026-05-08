package org.igye.remem3.app.controllers2.beans;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers2.beans.spel.CustomBeanExpressionResolver;
import org.igye.remem3.app.state.StateConstructor;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.Map;

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
        MutablePropertySources sources = ctx.getEnvironment().getPropertySources();
        sources.addLast(new MapPropertySource("props-from-main-context", Map.of(
            "app.beans-file", settings.getBeansFile()
        )));
        ctx.register(CustomBeansConfig.class);
        ctx.refresh();
        return new BeansState(ctx);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
