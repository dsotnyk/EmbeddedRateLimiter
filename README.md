# What is this

**Rate Limiter** is an embedded in-memory rate limiter, where rate is calculated per provided **sessionId**. It is fast, simple and designed to work with thousands of threads.

API have next methods
- isAllowed(sessionID)

RateLimiter is built to guarantee that particular session will be allowed exactly specified amount of requests per specified period, no matter the threads count.    

On my i9-9900K
- with 2000 threads I got about **4.44M TPS**
- with 5000 threads - about **4.35M TPS**

**Use at your own risk, it's ALFA**

# Where to start reading

Code - [InMemorySingleNodeRateLimiter.java](https://github.com/dsotnyk/EmbeddedRateLimiter/blob/main/src/main/java/me/sotnyk/ratelimiter/InMemorySingleNodeRateLimiter.java)  
Tests - [InMemorySingleNodeRateLimiterTest.java](https://github.com/dsotnyk/EmbeddedRateLimiter/blob/main/src/test/java/me/sotnyk/ratelimiter/InMemorySingleNodeRateLimiterTest.java)

# Where to get

Compile with Maven on your own. **And sorry for the long tests, they verify Race Conditions.**

# What's here

- Simple MT implementation
- Non-blocking operations except at the edge of the block rotation.

# Readiness

**PROS**
- Build is good and test coverage is ok
- It is reliable and fast

**CONS**
- Checkstyle, PMD, SpotBugs yet
- No JARs in Maven Central Repository yet, working on it.
- Unit tests are **VERY LONG**

# Caveats

- No persistence yet
- No clustering yet
- No REST API or any form of RMI 
- No stand-alone version yet, just embedded
- No partitioning in any form **YET**
- Tests coverage still not the best, mostly functional and performance

# History

**June 20, 2023**

I was challenged to create RateLimiter in 45 mins. Next day I had 2 hours to write some tests to my solution and fix few issues.

So, here is the result. Total time for coding is about 1.5 hours and about 2 hours for tests.

# Performance

See Unit tests, they are very comprehensive and check race conditions and performance.

> ================= STABILITY TEST STARTED  
50 sessions  
100 threads per session  
**5000 total threads**  
**15000 requests per thread**  
Period (bucket) size is set to 2 seconds  
**Rate per period per session is limited to 300**  
==================== Run 0 started at 1687406520000 on timeblock 843703260  
BUCKET ROTATED at 1687406520004  
BUCKET ROTATED at 1687406522000  
BUCKET ROTATED at 1687406524000  
BUCKET ROTATED at 1687406526000  
BUCKET ROTATED at 1687406528000  
BUCKET ROTATED at 1687406530000  
BUCKET ROTATED at 1687406532000  
BUCKET ROTATED at 1687406534000  
BUCKET ROTATED at 1687406536000  
Slow or late session detected, from 1687406535904 to 1687406536362 is 2 timeblocks. Allowed 600, rejected 1499400. Average rate 300  
Slow or late session detected, from 1687406523826 to 1687406524265 is 2 timeblocks. Allowed 600, rejected 1499400. Average rate 300  
Slow or late session detected, from 1687406525738 to 1687406526195 is 2 timeblocks. Allowed 600, rejected 1499400. Average rate 300  
Slow or late session detected, from 1687406521810 to 1687406522278 is 2 timeblocks. Allowed 600, rejected 1499400. Average rate 300  
Slow or late session detected, from 1687406527584 to 1687406528069 is 2 timeblocks. Allowed 600, rejected 1499400. Average rate 300  
Slow or late session detected, from 1687406527939 to 1687406528343 is 2 timeblocks. Allowed 600, rejected 1499400. Average rate 300  
Slow or late session detected, from 1687406529598 to 1687406530002 is 2 timeblocks. Allowed 315, rejected 1499685. Average rate 157  
Slow or late session detected, from 1687406529938 to 1687406530386 is 2 timeblocks. Allowed 600, rejected 1499400. Average rate 300  
Slow or late session detected, from 1687406531870 to 1687406532351 is 2 timeblocks. Allowed 600, rejected 1499400. Average rate 300  
Slow or late session detected, from 1687406533655 to 1687406534137 is 2 timeblocks. Allowed 600, rejected 1499400. Average rate 300  
==================== Run 0 ended at 1687406537299. 1000 threads out of 5000 was delayed or slow. **Performance is 4,336,000 TPS**  
40 session were computed in SINGLE period and each session (100 threads) was allowed for **EXACTLY 300 requests**  
10 session were slow and was computed in a FEW periods and each session (100 threads) had **AVERAGE allow rate no more than 300 requests**  
==================== Run 1 started at 1687406538001 on timeblock 843703269  
BUCKET ROTATED at 1687406538001  
BUCKET ROTATED at 1687406540000  
BUCKET ROTATED at 1687406542000  
BUCKET ROTATED at 1687406544000  
BUCKET ROTATED at 1687406546000  
BUCKET ROTATED at 1687406548000  
BUCKET ROTATED at 1687406550000  
BUCKET ROTATED at 1687406552000  
BUCKET ROTATED at 1687406554000  
Slow or late session detected, from 1687406553758 to 1687406554168 is 2 timeblocks. Allowed 600, rejected 1499400. Average rate 300  
Slow or late session detected, from 1687406541584 to 1687406542040 is 2 timeblocks. Allowed 600, rejected 1499400. Average rate 300  
Slow or late session detected, from 1687406541941 to 1687406542409 is 2 timeblocks. Allowed 600, rejected 1499400. Average rate 300  
Slow or late session detected, from 1687406543699 to 1687406544153 is 2 timeblocks. Allowed 600, rejected 1499400. Average rate 300  
Slow or late session detected, from 1687406539763 to 1687406540221 is 2 timeblocks. Allowed 600, rejected 1499400. Average rate 300  
Slow or late session detected, from 1687406545796 to 1687406546270 is 2 timeblocks. Allowed 600, rejected 1499400. Average rate 300  
Slow or late session detected, from 1687406547537 to 1687406548007 is 2 timeblocks. Allowed 600, rejected 1499400. Average rate 300  
Slow or late session detected, from 1687406547888 to 1687406548353 is 2 timeblocks. Allowed 600, rejected 1499400. Average rate 300  
Slow or late session detected, from 1687406549653 to 1687406550148 is 2 timeblocks. Allowed 600, rejected 1499400. Average rate 300  
Slow or late session detected, from 1687406551820 to 1687406552261 is 2 timeblocks. Allowed 600, rejected 1499400. Average rate 300  
==================== Run 1 ended at 1687406555538. 1000 threads out of 5000 was delayed or slow. **Performance is 4,276,000 TPS**  
40 session were computed in SINGLE period and each session (100 threads) was allowed for **EXACTLY 300 requests**  
10 session were slow and was computed in a FEW periods and each session (100 threads) had **AVERAGE allow rate no more than 300 requests**  

I believe TPS is limited by my test threads (see [InMemorySingleNodeRateLimiterTest.java](https://github.com/dsotnyk/EmbeddedRateLimiter/blob/main/src/test/java/me/sotnyk/ratelimiter/InMemorySingleNodeRateLimiterTest.java)), cause RateLimiter was designed in a way to be insensitive to multi-threading at all, but I see slight TPS degradation with 2000 to 5000 threads. 

**I'd expect 10M+ TPS if rate limiter will be deployed separately from test load.**

# Future

C'mon, this is just a 2 hours fun!

# License

GPL v3, if you need something else - let me know.