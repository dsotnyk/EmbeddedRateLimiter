package me.sotnyk.ratelimiter.properties;

public interface PropertyProvider {
    /**
     * @return How many requests per period per user session allowed
     */
    int getLimit();

    /**
     * @return period size in seconds
     */
    int getPeriod();

    /**
     * @return Read lock timeout (to lock bucket to read)
     */
    int getReadLockTolerance();
    /**
     * @return Write lock timeout (to refresh bucket)
     */
    int getWriteLockTolerance();
}
