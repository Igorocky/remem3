package org.igye.remem3.app.controllers2.beans;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.FileSystemXmlApplicationContext;

@RequiredArgsConstructor
@Getter
public class BeansState {
    private final FileSystemXmlApplicationContext ctx;
}
