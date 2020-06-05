package fredboat.testutil

import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger(RetryableTest::class.java)

interface RetryableTest {

    fun beforeRetry()

    fun retryable(maxAttempts: Int = 5, minSuccessful: Int = 2, test: () -> Unit) {
        var successful = 0
        for (attempt in 1..maxAttempts) {
            try {
                test()
                successful++
                if (successful == minSuccessful) return
            } catch (t: Throwable) {
                log.error("Test attempt $attempt failed", t)
                if (attempt == maxAttempts) throw t
            }
        }

        if (successful < minSuccessful) {
            throw IllegalStateException("Required at least $minSuccessful successes, got $successful")
        }
    }

}