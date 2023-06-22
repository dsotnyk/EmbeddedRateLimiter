package me.sotnyk.ratelimiter.exception;

public class GenericFailureException extends RuntimeException {
    public GenericFailureException(Throwable cause) {
        super(cause);
    }

}
