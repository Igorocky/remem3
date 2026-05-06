package org.igye.remem3.app.controllers2.beans;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.state.StateConstructor;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.convert.ConversionService;

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
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(CustomBeansConfig.class);
        ctx.getBeanFactory().setBeanExpressionResolver(new CustomBeanExpressionResolver());
        new XmlBeanDefinitionReader(ctx).loadBeanDefinitions("file:" + settings.getBeansFile());
        Functions.setConversionService(ctx.getBean(ConversionService.class));
        return new BeansState(ctx);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
