package me.sotnyk.ratelimiter;

import java.util.concurrent.Callable;

public class SimpleTestRunner implements Callable<ExecutionStatus> {

    private final int count;
    RateLimiter limiter;
    String sessionId;


    public SimpleTestRunner(RateLimiter limiter, String sessionId, int count) {
        this.limiter = limiter;
        this.sessionId = sessionId;
        this.count = count;
    }

    @Override
    public ExecutionStatus call() throws Exception {

        var stat = new ExecutionStatus();
        stat.sessionId = sessionId;

        stat.firstCall = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            if (limiter.isAllowed(sessionId)) stat.pos++; else stat.neg++;
        }

        stat.lastCall = System.currentTimeMillis();

        stat.startBlock = stat.firstCall / (limiter.getPeriod() * 1000L);
        stat.endBlock = stat.lastCall / (limiter.getPeriod() * 1000L);
        stat.blocks = stat.endBlock - stat.startBlock + 1;

        return stat;
    }
}
