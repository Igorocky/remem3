package org.igye.remem3.app.controllers2.beans;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@RequiredArgsConstructor
@Getter
public class BeansState {
    private final AnnotationConfigApplicationContext ctx;
}
