package org.igye.remem3.app.controllers2.beans;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.state.StateConstructor;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@RequiredArgsConstructor
public class BeansConstructor implements StateConstructor<BeansState> {
    private final Settings settings;

    @Override
    public String getName() {
        return "beans";
    }

    @Override
    public BeansState construct() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(CustomBeansConfig.class);
        ctx.getBeanFactory().setBeanExpressionResolver(new CustomBeanExpressionResolver());
        new XmlBeanDefinitionReader(ctx).loadBeanDefinitions("file:" + settings.getBeansFile());
        return new BeansState(ctx);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
