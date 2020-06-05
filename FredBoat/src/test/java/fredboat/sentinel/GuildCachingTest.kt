package fredboat.sentinel

import com.fredboat.sentinel.entities.GuildSubscribeRequest
import fredboat.testutil.IntegrationTest
import fredboat.testutil.sentinel.SentinelState
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test

class GuildCachingTest : IntegrationTest() {

    @Test
    fun responseGetsCached() {
        for (i in 1..10) {
            val ctx = commandTester.parseContext(";;say test")
            print(ctx.command.toString())
        }
        Thread.sleep(100)
        assertEquals("Expected only one subscribe request", 1, SentinelState.outgoing[GuildSubscribeRequest::class.java]?.size)
    }

}