package fredboat.command.moderation

import com.fredboat.sentinel.entities.ModRequestType
import fredboat.testutil.sentinel.Raws
import fredboat.testutil.sentinel.SentinelState
import fredboat.testutil.sentinel.assertReply
import fredboat.testutil.sentinel.assertReplyContains
import org.junit.jupiter.api.Test

class UnbanCommandTest : AbstractModerationTest() {

    private val target = Raws.banList[0].user

    @Test
    fun testWorking() {
        testCommand(";;unban ${target.name}", invoker = Raws.napster) {
            expect(ModRequestType.UNBAN, target.id)
        }
    }

    @Test
    fun testWorkingOwner() {
        testCommand(";;unban ${target.id}", invoker = Raws.owner) {
            expect(ModRequestType.UNBAN, target.id)
        }
    }

    @Test
    fun testUnprivileged() {
        testCommand(message = ";;unban ${target.id}", invoker = Raws.realkc) {
            assertReply("You need the following permission to perform that action: **Ban Members**")
        }
    }

    @Test
    fun testBotUnprivileged() {
        SentinelState.setRoles(member = Raws.self, roles = emptyList())
        testCommand(message = ";;unban ${target.id}", invoker = Raws.napster) {
            assertReplyContains("I need the following permission to perform that action: **Ban Members**")
        }
    }

}