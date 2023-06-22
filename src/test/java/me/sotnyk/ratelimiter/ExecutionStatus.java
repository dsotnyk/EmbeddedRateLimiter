package me.sotnyk.ratelimiter;

public class ExecutionStatus {
    public long startBlock;
    public long endBlock;
    public long blocks;
    int pos = 0;
    int neg = 0;

    long firstCall;

    long lastCall;

    String sessionId;
}
