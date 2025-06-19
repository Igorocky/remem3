package org.igye.remem3.utils;

public class Exn extends RuntimeException {
    public Exn(String message) {
        super(message);
    }

    public Exn(String message, Throwable cause) {
        super(message, cause);
    }

    public Exn(Throwable cause) {
        super(cause);
    }
}
