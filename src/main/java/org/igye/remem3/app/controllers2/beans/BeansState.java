package org.igye.remem3.app.controllers2.beans;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.utils.Exn;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Getter
public class BeansState {
    private final AnnotationConfigApplicationContext ctx;

    public <T> List<Pair<String, T>> getBeans(Class<T> type) {
        return Arrays.stream(ctx.getBeanDefinitionNames())
            .map(name -> Pair.of(name, ctx.getBean(name)))
            .filter(pair -> type.isAssignableFrom(pair.getRight().getClass()))
            .map(pair -> Pair.of(pair.getLeft(), (T) pair.getRight()))
            .toList();
    }

    public <T> Pair<String, T> getBean(Class<T> type) {
        List<Pair<String, T>> beans = getBeans(type);
        if (beans.size() != 1) {
            throw new Exn("Expected to find exactly 1 bean of type %s, but found %s".formatted(
                type, beans.size()
            ));
        }
        return beans.getFirst();
    }
}
