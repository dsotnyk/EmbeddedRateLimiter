package me.sotnyk.ratelimiter;

import me.sotnyk.ratelimiter.exception.GenericFailureException;
import me.sotnyk.ratelimiter.exception.OverloadException;
import me.sotnyk.ratelimiter.properties.PropertyProvider;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InMemorySingleNodeRateLimiter implements RateLimiter {

    private final PropertyProvider propertyProvider;

    private ConcurrentHashMap<String, AtomicInteger> bucket = new ConcurrentHashMap<>();

    private final ReadWriteLock bucketLock = new ReentrantReadWriteLock();

    private volatile long bucketCreationTime = System.currentTimeMillis();

    static volatile RateLimiter instance;

    // Contrustor guaranteed to be only-once
    private InMemorySingleNodeRateLimiter(PropertyProvider propertyProvider) {
        this.propertyProvider = propertyProvider;
    }

    /**
     * Rate limiter singleton
     *
     * @return rate limiter instance
     * @throws IllegalStateException if limiter was not initialized
     */
    public static RateLimiter getInstance() {
        if (instance != null) return instance;
        else throw new IllegalStateException("Rate limiter was not properly initialized");
    }

    /**
     * This method initializes Rate limiter for JVM context.
     * Application must init it on startup to make it globally available
     *
     * @return true, if initialization completed and false if already initialized
     */
    public static boolean initialize(PropertyProvider propertyProvider) {
        if (instance != null) return false;

        synchronized (InMemorySingleNodeRateLimiter.class) {
            if (instance != null) return false;

            instance = new InMemorySingleNodeRateLimiter(propertyProvider);
            return true;
        }
    }


    /**
     * Method is trying to check is request allowed.
     * @param sessionId Session ID to calculate limit
     * @return Is request allowed (didn't hit limit yet)
     */
    public boolean isAllowed(String sessionId) {
        // get recent bucket
        var currentBucket = getCurrentBucket();

        return currentBucket.computeIfAbsent(sessionId, a -> new AtomicInteger(0)).incrementAndGet() <= propertyProvider.getLimit();
    }

    @Override
    public int getPeriod() {
        return propertyProvider.getPeriod();
    }

    @Override
    public int getLimit() {
        return propertyProvider.getLimit();
    }

    /**
     * Returns is bucket stale. NOT THREAD SAFE, MUST BE INVOKED FROM SYNCHRONIZED ENVIRONMENT
     *
     * @return Is bucket stale
     */
    private boolean isBucketStale() {
        return this.bucketCreationTime / (propertyProvider.getPeriod() * 1000L) != System.currentTimeMillis() / (propertyProvider.getPeriod() * 1000L);
    }

    private ConcurrentHashMap<String, AtomicInteger> getCurrentBucket() {
        try {
            // try to lock the bucket from change and check is it stale
            if (!bucketLock.readLock().tryLock(propertyProvider.getReadLockTolerance(), TimeUnit.MILLISECONDS))
                throw new OverloadException();
            try {
                if (!isBucketStale()) return this.bucket;
            } finally {
                bucketLock.readLock().unlock(); // we MUST do it to avoid deadlock
            }

            // okay we are still here? bucket stale
            // try to lock to change and return
            if (!bucketLock.writeLock().tryLock(propertyProvider.getWriteLockTolerance(), TimeUnit.MILLISECONDS))
                throw new OverloadException();

            // DOUBLE CHECK FOR RACE CONDITION
            if (!isBucketStale()) {
                bucketLock.writeLock().unlock();
                return this.bucket;
            }

            // NEVER EVER CLEAN THE BUCKET, CAUSE HANGED THREADS MAY STILL RELY ON LOCAL COPY
            // TODO: Remove or switch to Logger
            System.out.println("BUCKET ROTATED at "+ System.currentTimeMillis());
            bucket = new ConcurrentHashMap<>();
            bucketCreationTime = System.currentTimeMillis();
            bucketLock.writeLock().unlock();
            return this.bucket;

        } catch (InterruptedException e) {
            throw new GenericFailureException(e);
        }
    }


}
