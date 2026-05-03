package org.igye.remem3.web;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.igye.remem3.html.HtmlBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestControllerExceptionHandler extends HtmlBuilder {


    @ExceptionHandler(Throwable.class)
    public ResponseEntity<?> process(Throwable throwable) {
        return ResponseEntity
            .status(500)
            .contentType(MediaType.TEXT_HTML)
            .body(
                simplePageWithTitle("Error",
                    frag(
                        h3(div("color:red;", text("Error"))),
                        text(throwable.getMessage()),
                        pre(text(ExceptionUtils.getStackTrace(throwable)))
                    )
                ).toString()
            );
    }
}
