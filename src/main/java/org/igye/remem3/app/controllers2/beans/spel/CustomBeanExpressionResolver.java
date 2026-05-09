package org.igye.remem3.app.controllers2.beans.spel;

import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class CustomBeanExpressionResolver extends StandardBeanExpressionResolver {

    @Override
    protected void customizeEvaluationContext(StandardEvaluationContext evalContext) {
        super.customizeEvaluationContext(evalContext);
        evalContext.setVariable("SPeL", "org.igye.remem3.app.controllers2.beans.spel.SpelFactoryBean");
        evalContext.setVariable("Exercise", "org.igye.remem3.app.controllers2.beans.dto.ExerciseDef$SimpleExerciseDef");
        evalContext.setVariable("Circle", "org.igye.remem3.app.controllers2.beans.dto.RepeatStrategyParams$RepeatStrategyCircleParams");
        Functions.registerFunctions(evalContext);
    }
}
