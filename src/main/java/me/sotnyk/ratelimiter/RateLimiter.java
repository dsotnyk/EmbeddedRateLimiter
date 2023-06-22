package me.sotnyk.ratelimiter;

public interface RateLimiter {
    boolean isAllowed(String sessionId);

    int getPeriod();

    int getLimit();
}
