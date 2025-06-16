package org.igye.remem3.utils;

public class RememExn extends RuntimeException {
    public RememExn(String message) {
        super(message);
    }

    public RememExn(String message, Throwable cause) {
        super(message, cause);
    }

    public RememExn(Throwable cause) {
        super(cause);
    }
}
