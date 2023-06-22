package me.sotnyk.ratelimiter.properties;

public class DefaultPropertyProvider implements PropertyProvider {
    @Override
    public int getLimit() {
        return 300;
    }

    @Override
    public int getPeriod() {
        return 2;
    }

    @Override
    public int getReadLockTolerance() {
        return 100;
    }

    @Override
    public int getWriteLockTolerance() {
        return 200;
    }
}
