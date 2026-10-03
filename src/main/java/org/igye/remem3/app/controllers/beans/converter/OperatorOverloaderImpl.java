package org.igye.remem3.app.controllers.beans.converter;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.devtoolsgroup.simplespelshell.BasicOperatorOverloader;
import org.igye.remem3.app.TaskTypeMatcher;
import org.igye.remem3.app.dto.TaskType;
import org.igye.remem3.utils.Exn;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class OperatorOverloaderImpl extends BasicOperatorOverloader {
    private final ConversionService conversionService;
    private final ConcurrentHashMap<String, TaskTypeMatcher> taskTypeMatchers = new ConcurrentHashMap<>();

    @Override
    public boolean overridesOperation(
        Operation operation, @Nullable Object leftOperand, @Nullable Object rightOperand
    ) throws EvaluationException {
        return operation == Operation.MODULUS && leftOperand instanceof TaskType
            && (rightOperand instanceof String || rightOperand instanceof List<?>)
            || super.overridesOperation(operation, leftOperand, rightOperand);
    }

    @Override
    public Object operate(
        Operation operation, @Nullable Object leftOperand, @Nullable Object rightOperand
    ) throws EvaluationException {
        if (operation == Operation.MODULUS && leftOperand instanceof TaskType left) {
            if (rightOperand instanceof String right) {
                return strToTaskTypeMatcher(right).matches(left);
            } else if (rightOperand instanceof List<?> right) {
                return listToTaskTypeMatcher(right).matches(left);
            }
        }
        return super.operate(operation, leftOperand, rightOperand);
    }

    private TaskTypeMatcher listToTaskTypeMatcher(List<?> list) {
        String key = StringUtils.join(",");
        TaskTypeMatcher res = taskTypeMatchers.get(key);
        if (res == null) {
            List<TaskTypeMatcher> matchers = (List<TaskTypeMatcher>) conversionService.convert(
                list,
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(TaskTypeMatcher.class))
            );
            res = matchers.stream().reduce(TaskTypeMatcher::or).orElseThrow(() ->
                new Exn("Cannot convert '%s' to a TaskTypeMatcher.".formatted(key))
            );
            taskTypeMatchers.put(key, res);
        }
        return res;
    }

    private TaskTypeMatcher strToTaskTypeMatcher(String str) {
        TaskTypeMatcher taskTypeMatcher = taskTypeMatchers.get(str);
        return taskTypeMatcher != null ? taskTypeMatcher : listToTaskTypeMatcher(List.of(str));
    }
}
