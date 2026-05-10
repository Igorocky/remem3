package org.igye.remem3.app.controllers.beans;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.controllers.beans.spel.CustomBeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@RequiredArgsConstructor
public class CustomApplicationContext extends AnnotationConfigApplicationContext {
    private final CustomBeanExpressionResolver customBeanExpressionResolver;

    @Override
    protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        super.prepareBeanFactory(beanFactory);
        beanFactory.setBeanExpressionResolver(customBeanExpressionResolver);
    }
}