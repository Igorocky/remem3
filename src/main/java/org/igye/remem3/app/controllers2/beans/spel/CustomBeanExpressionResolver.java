package org.igye.remem3.app.controllers2.beans.spel;

import org.igye.remem3.app.controllers2.beans.dto.ExerciseDef;
import org.igye.remem3.app.controllers2.beans.dto.RepeatStrategyParams;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class CustomBeanExpressionResolver extends StandardBeanExpressionResolver {

    @Override
    protected void customizeEvaluationContext(StandardEvaluationContext evalCtx) {
        super.customizeEvaluationContext(evalCtx);
        regClassName(evalCtx, "SPeL", SpelFactoryBean.class);
        regClassName(evalCtx, "Circle", RepeatStrategyParams.RepeatStrategyCircleParams.class);
        regClassName(evalCtx, "Queue", RepeatStrategyParams.RepeatStrategyQueueParams.class);
        regClassName(evalCtx, "Buckets", RepeatStrategyParams.RepeatStrategyBucketsParams.class);
        regClassName(evalCtx, "Exercise", ExerciseDef.SimpleExerciseDef.class);
        Functions.registerFunctions(evalCtx);
    }

    private void regClassName(StandardEvaluationContext evalContext, String shortName, Class<?> clas) {
        evalContext.setVariable(shortName, clas.getCanonicalName());
    }
}
