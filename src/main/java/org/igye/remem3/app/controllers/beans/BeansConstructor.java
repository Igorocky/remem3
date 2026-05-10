package org.igye.remem3.app.controllers.beans;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers.beans.spel.CustomBeanExpressionResolver;
import org.igye.remem3.app.state.StateConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Order(1000)
public class BeansConstructor implements StateConstructor<BeansState> {
    public static final String BEANS = "beans";
    private final Settings settings;
    private final Environment environment;

    @Override
    public String getName() {
        return BEANS;
    }

    @Override
    public String getDisplayName() {
        return "Beans";
    }

    @Override
    public BeansState construct() {
        CustomApplicationContext ctx = new CustomApplicationContext(new CustomBeanExpressionResolver());
        MutablePropertySources sources = ctx.getEnvironment().getPropertySources();
        Map<String, Object> propsToPassToBeans = new HashMap<>();
        propsToPassToBeans.put("app.beans-file", settings.getBeansFile());
        for (String propToPassToBeans : settings.getPropsToPassToBeans()) {
            propsToPassToBeans.put(propToPassToBeans, environment.getProperty(propToPassToBeans, Object.class));
        }
        sources.addLast(new MapPropertySource("props-from-main-context", propsToPassToBeans));
        ctx.register(CustomBeansConfig.class);
        ctx.refresh();
        return new BeansState(ctx);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
