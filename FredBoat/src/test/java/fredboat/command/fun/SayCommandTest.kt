package fredboat.command.`fun`

import fredboat.testutil.IntegrationTest
import fredboat.testutil.sentinel.assertReply
import fredboat.util.TextUtils.ZERO_WIDTH_CHAR
import org.junit.jupiter.api.Test

class SayCommandTest : IntegrationTest() {
    @Test
    fun onInvoke() {
        testCommand(";;say qwe rty") {
            assertReply(ZERO_WIDTH_CHAR + "qwe rty")
        }
    }

    @Test
    fun testSanitized() {
        testCommand(";;say @everyone") {
            assertReply("$ZERO_WIDTH_CHAR@${ZERO_WIDTH_CHAR}everyone")
        }

        testCommand(";;say @here") {
            assertReply("$ZERO_WIDTH_CHAR@${ZERO_WIDTH_CHAR}here")
        }
    }
}