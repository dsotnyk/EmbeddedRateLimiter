package me.sotnyk.ratelimiter;

import me.sotnyk.ratelimiter.properties.DefaultPropertyProvider;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.*;

public class InMemorySingleNodeRateLimiterTest {

    /**
     * Simple single-thread run for LIMIT requests and then for another LIMIT requests with the same session ID
     */
    @Test
    public void testHappyPathSequentialSamePeriod() throws InterruptedException {
        // init
        var pp = new DefaultPropertyProvider();
        // wait till period starts
        Thread.sleep(pp.getPeriod() * 1000L - System.currentTimeMillis() % (pp.getPeriod() * 1000L));

        InMemorySingleNodeRateLimiter.initialize(pp);

        // Run till limit
        for (int i = 0; i < pp.getLimit(); i++) {
            Assert.assertTrue(InMemorySingleNodeRateLimiter.getInstance().isAllowed("suryfcv384t"));
        }

        // Run over limit
        for (int i = 0; i < pp.getLimit(); i++) {
            Assert.assertFalse(InMemorySingleNodeRateLimiter.getInstance().isAllowed("suryfcv384t"));
        }
    }

    /**
     * Test bucket expiration
     */
    @Test
    public void testBucketRotateSequentialSinglePeriod() throws InterruptedException {
        // init
        var pp = new DefaultPropertyProvider();
        // wait till period starts
        Thread.sleep(pp.getPeriod() * 1000L - System.currentTimeMillis() % (pp.getPeriod() * 1000L));

        InMemorySingleNodeRateLimiter.initialize(pp);

        // Run to limit
        for (int i = 0; i < pp.getLimit(); i++) {
            Assert.assertTrue(InMemorySingleNodeRateLimiter.getInstance().isAllowed("suryfcv384t"));
        }

        // Run over limit
        for (int i = 0; i < pp.getLimit(); i++) {
            Assert.assertFalse(InMemorySingleNodeRateLimiter.getInstance().isAllowed("suryfcv384t"));
        }

        // wait for expiration
        Thread.sleep(pp.getPeriod() * 1000L);

        // try again, run to limit
        for (int i = 0; i < pp.getLimit(); i++) {
            Assert.assertTrue(InMemorySingleNodeRateLimiter.getInstance().isAllowed("suryfcv384t"));
        }
    }

    /**
     * Run multiple different sessions in parallel. Each session is sequential, but runs in separate thread.
     * SINGLE Run
     */
    @Test
    public void testHappyPathParallelSinglePeriod() throws InterruptedException, ExecutionException {
        int THREADS = 1000;
        int MULTIPLIER = 10;

        // init
        var pp = new DefaultPropertyProvider();

        ExecutorService executorService = Executors.newFixedThreadPool(THREADS);

        var tasks = new ArrayList<Callable<ExecutionStatus>>(THREADS);


        // wait till period starts
        Thread.sleep(pp.getPeriod() * 1000L - System.currentTimeMillis() % (pp.getPeriod() * 1000L));

        InMemorySingleNodeRateLimiter.initialize(pp);

        for (int i = 0; i < THREADS; i++) {
            tasks.add(new SimpleTestRunner(InMemorySingleNodeRateLimiter.getInstance(), Integer.toString(i), pp.getLimit() * MULTIPLIER));
        }

        var results = executorService.invokeAll(tasks);

        for (var result : results) {
            var stat = result.get();
            Assert.assertEquals(stat.pos, pp.getLimit());
            Assert.assertEquals(stat.neg, pp.getLimit() * (MULTIPLIER - 1));
        }
    }

    /**
     * Run multiple different sessions in parallel for stability. Each session is sequential, but runs in separate thread.
     * MULTIPLE RUNS
     */
    @Test
    public void testSimpleStability() throws InterruptedException, ExecutionException {
        int THREADS = 2000; // sessions to simulate
        int PERIODS = 50; // periods to test
        int MULTIPLIER = 50; // requests = limit * multiplier, be reasonable to fit into period
        boolean DEBUG = true;

        var d = new DebugWriter(DEBUG);

        // init limiter
        var pp = new DefaultPropertyProvider();
        InMemorySingleNodeRateLimiter.initialize(pp);

        d
                .twit(" ================= STABILITY TEST STARTED ")
                .twit(THREADS + " threads")
                .twit(MULTIPLIER * pp.getLimit() + " requests per thread");

        // init threads
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        var tasks = new ArrayList<Callable<ExecutionStatus>>(THREADS);
        for (int i = 0; i < THREADS; i++) {
            tasks.add(new SimpleTestRunner(InMemorySingleNodeRateLimiter.getInstance(), Integer.toString(i), pp.getLimit() * MULTIPLIER));
        }

        // calculate all periods and verify
        for (int r = 0; r < PERIODS; r++) {
            // wait till period starts
            Thread.sleep(pp.getPeriod() * 1000L - System.currentTimeMillis() % (pp.getPeriod() * 1000L));

            d.twit("==================== EXECUTION " + r + " started at " + System.currentTimeMillis() + " on timeblock " + System.currentTimeMillis() / (pp.getPeriod() * 1000L));

            var results = executorService.invokeAll(tasks);

            long maxTime = System.currentTimeMillis();
            long minTime = maxTime;
            int fast = 0;
            int slow = 0;

            for (var result : results) {
                var stat = result.get();

                if (stat.blocks == 1) {
                    // for fast threads
                    Assert.assertEquals(stat.pos, pp.getLimit());
                    Assert.assertEquals(stat.neg, pp.getLimit() * (MULTIPLIER - 1));
                    fast++;
                } else {
                    // for slow or delayed threads
                    d.twit("Slow or late thread detected, from " + stat.firstCall + " to " + stat.lastCall + " is " + stat.blocks + " timeblocks. " + "Allowed " + stat.pos + ", rejected " + stat.neg + ". Average rate " + stat.pos / stat.blocks);
                    Assert.assertTrue(stat.pos / stat.blocks <= pp.getLimit());
                    slow++;
                }

                maxTime = Math.max(maxTime, stat.lastCall);
                minTime = Math.min(minTime, stat.firstCall);
            }

            d.twit("==================== EXECUTION " + r + " COMPLETE at " + maxTime + ". " + slow + " threads out of " + THREADS + " was delayed or slow. Performance is " + (long) pp.getLimit() * MULTIPLIER * THREADS / (maxTime - minTime) * 1000 + " TPS");
        }
    }

    /**
     * Run multiple different sessions in parallel for stability. TRUE emulation, each session is run in parallel
     * This is the most comprehensive test which will reveal all possible race conditions. It's LONG (20 mins on i9-9900K)
     */
    @Test
    public void testComprehensive() throws InterruptedException, ExecutionException {
        int SESSIONS = 50; // sessions to simulate
        int THREADS_PER_SESSIONS = 100; // concurrency per session
        int PERIODS = 50; // periods to test
        int MULTIPLIER = 50; // requests = limit * multiplier, be reasonable to fit into period
        boolean DEBUG = true;

        var d = new DebugWriter(DEBUG);

        // init limiter
        var pp = new DefaultPropertyProvider();
        InMemorySingleNodeRateLimiter.initialize(pp);

        d
                .twit(" ================= STABILITY TEST STARTED ")
                .twit(SESSIONS + " sessions")
                .twit(THREADS_PER_SESSIONS + " threads per session")
                .twit(THREADS_PER_SESSIONS * SESSIONS + " total threads")
                .twit(MULTIPLIER * pp.getLimit() + " requests per thread")
                .twit("Period (bucket) size is set to " + pp.getPeriod() + " seconds")
                .twit("Rate per period per session is limited to " + pp.getLimit());

        // init threads
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        var tasks = new ArrayList<Callable<ExecutionStatus>>(THREADS_PER_SESSIONS * SESSIONS);
        for (int i = 0; i < SESSIONS; i++) {
            for (int j = 0; j < THREADS_PER_SESSIONS; j++) {
                tasks.add(new SimpleTestRunner(InMemorySingleNodeRateLimiter.getInstance(), Integer.toString(i), pp.getLimit() * MULTIPLIER));
            }
        }

        // run X times
        for (int r = 0; r < PERIODS; r++) {
            // wait till period starts
            Thread.sleep(pp.getPeriod() * 1000L - System.currentTimeMillis() % (pp.getPeriod() * 1000L));

            d.twit("==================== Run " + r + " started at " + System.currentTimeMillis() + " on timeblock " + System.currentTimeMillis() / (pp.getPeriod() * 1000L));

            // run it!!!
            var results = executorService.invokeAll(tasks);

            long maxTime = System.currentTimeMillis();
            long minTime = maxTime;
            int fastThreads = 0;
            int slowThreads = 0;

            // merge all threads per sessionId
            var stats = extractStats(results);

            // verify
            for (var stat : stats) {

                if (stat.blocks == 1) {
                    // for fast threads
                    Assert.assertEquals(stat.pos, pp.getLimit());
                    Assert.assertEquals(stat.neg, pp.getLimit() * (THREADS_PER_SESSIONS * MULTIPLIER - 1));
                    fastThreads++;
                } else {
                    // for slow or delayed threads
                    d.twit("Slow or late session detected, from " + stat.firstCall + " to " + stat.lastCall + " is " + stat.blocks + " timeblocks. " + "Allowed " + stat.pos + ", rejected " + stat.neg + ". Average rate " + stat.pos / stat.blocks);
                    Assert.assertTrue(stat.pos / stat.blocks <= pp.getLimit());
                    slowThreads++;
                }

                maxTime = Math.max(maxTime, stat.lastCall);
                minTime = Math.min(minTime, stat.firstCall);
            }
            long tps = (long) pp.getLimit() * MULTIPLIER * THREADS_PER_SESSIONS * SESSIONS / (maxTime - minTime) * 1000;
            d
                    .twit("==================== Run " + r + " ended at " + maxTime + ". " + slowThreads * THREADS_PER_SESSIONS + " threads out of " + THREADS_PER_SESSIONS * SESSIONS + " was delayed or slow. Performance is " + tps + " TPS")
                    .twit(fastThreads + " session were computed in SINGLE period and each session (" + THREADS_PER_SESSIONS + " threads) was allowed for EXACTLY " + pp.getLimit() + " requests")
                    .twit(slowThreads + " session were slow and was computed in a FEW periods and each session (" + THREADS_PER_SESSIONS + " threads) had AVERAGE allow rate no more than " + pp.getLimit() + " requests");
        }
    }

    private Collection<ExecutionStatus> extractStats(List<Future<ExecutionStatus>> results) throws ExecutionException, InterruptedException {
        var m = new HashMap<String, ExecutionStatus>();

        for (var result : results) {
            var n = result.get();
            var c = m.get(n.sessionId);

            if (c == null) {
                m.put(n.sessionId, n);
            } else {
                c.pos += n.pos;
                c.neg += n.neg;
                c.firstCall = Math.min(c.firstCall, n.firstCall);
                c.lastCall = Math.max(c.lastCall, n.lastCall);
                c.startBlock = Math.min(c.startBlock, n.startBlock);
                c.endBlock = Math.max(c.endBlock, n.endBlock);
                c.blocks = c.endBlock - c.startBlock + 1;
            }
        }

        return m.values();
    }

}
