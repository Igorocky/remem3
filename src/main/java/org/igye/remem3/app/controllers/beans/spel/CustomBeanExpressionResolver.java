package org.igye.remem3.app.controllers.beans.spel;

import org.igye.remem3.app.controllers.beans.dto.ExerciseDef;
import org.igye.remem3.app.controllers.beans.dto.RepeatStrategyParams;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class CustomBeanExpressionResolver extends StandardBeanExpressionResolver {

    @Override
    protected void customizeEvaluationContext(StandardEvaluationContext evalCtx) {
        super.customizeEvaluationContext(evalCtx);
        regClassName(evalCtx, "SPeL", SpelFactoryBean.class);
        regClassName(evalCtx, "Circle", RepeatStrategyParams.RepeatStrategyCircleParams.class);
        regClassName(evalCtx, "RetryFailed", RepeatStrategyParams.RepeatStrategyRetryFailedParams.class);
        regClassName(evalCtx, "Queue", RepeatStrategyParams.RepeatStrategyQueueParams.class);
        regClassName(evalCtx, "Buckets", RepeatStrategyParams.RepeatStrategyBucketsParams.class);
        regClassName(evalCtx, "Exercise", ExerciseDef.SimpleExerciseDef.class);
        regClassName(evalCtx, "CompoundExercise", ExerciseDef.CompoundExerciseDef.class);
        Functions.registerFunctions(evalCtx);
    }

    private void regClassName(StandardEvaluationContext evalContext, String shortName, Class<?> clas) {
        evalContext.setVariable(shortName, clas.getCanonicalName());
    }
}
