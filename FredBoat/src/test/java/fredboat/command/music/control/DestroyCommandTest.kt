package fredboat.command.music.control

import fredboat.testutil.IntegrationTest
import fredboat.testutil.sentinel.assertReply
import fredboat.testutil.util.cachedGuild
import fredboat.testutil.util.queue
import org.junit.jupiter.api.Test

internal class DestroyCommandTest : IntegrationTest() {

    @Test
    fun onInvoke() {
        cachedGuild.queue(PlayCommandTest.url)
        testCommand(";;Destroy") {
            assertReply("${member.effectiveName}:  Reset the player and cleared the queue.")
        }
    }
}