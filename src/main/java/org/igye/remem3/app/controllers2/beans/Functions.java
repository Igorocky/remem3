package org.igye.remem3.app.controllers2.beans;

import org.igye.remem3.app.TaskTypeMatcher;
import org.igye.remem3.app.controllers2.beans.dto.TaskFilter;
import org.igye.remem3.app.impl.TaskTypeMatcherImpl;
import org.igye.remem3.utils.Exn;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public class Functions {
    protected static volatile ConversionService conversionService;

    public static void setConversionService(ConversionService conversionService) {
        Functions.conversionService = conversionService;
    }

    public static TaskFilter taskTypeIn(String... types) {
        TaskTypeMatcher taskTypeMatcher = Arrays.stream(types)
            .map(TaskTypeMatcherImpl::new)
            .map(TaskTypeMatcher.class::cast)
            .reduce(TaskTypeMatcher::or)
            .get();
        return task -> taskTypeMatcher.matches(task.getTaskType());
    }

    private static <T> Optional<T> getParamOpt(Map<String, Object> params, String name, Class<T> type) {
        return Optional.ofNullable(conversionService.convert(params.get(name), type));
    }

    private static <T> T getParam(Map<String, Object> params, String name, Class<T> type) {
        return getParamOpt(params, name, type).orElseThrow(() -> new Exn(
            "Parameter '%s' is required.".formatted(name)
        ));
    }

    private static <T> Optional<T> getParamOpt(Map<String, Object> params, String name, TypeDescriptor type) {
        return Optional.ofNullable((T) conversionService.convert(params.get(name), type));
    }

    private static <T> T getParam(Map<String, Object> params, String name, TypeDescriptor type) {
        return ((Optional<T>) getParamOpt(params, name, type)).orElseThrow(() -> new Exn(
            "Parameter '%s' is required.".formatted(name)
        ));
    }
}
