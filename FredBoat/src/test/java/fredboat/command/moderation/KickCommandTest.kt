package fredboat.command.moderation

import com.fredboat.sentinel.entities.ModRequestType
import fredboat.testutil.sentinel.Raws
import fredboat.testutil.sentinel.SentinelState
import fredboat.testutil.sentinel.assertReply
import fredboat.testutil.sentinel.assertReplyContains
import org.junit.jupiter.api.Test

class KickCommandTest : AbstractModerationTest() {

    @Test
    fun testWorking() {
        testCommand(message = ";;kick realkc Generic shitposting", invoker = Raws.napster) {
            expect(ModRequestType.KICK, Raws.realkc, "Generic shitposting")
        }
    }

    @Test
    fun testWorkingOwner() {
        testCommand(message = ";;kick napster Generic shitposting", invoker = Raws.owner) {
            expect(ModRequestType.KICK, Raws.napster, "Generic shitposting")
        }
    }

    @Test
    fun testUnprivileged() {
        testCommand(message = ";;kick napster Generic shitposting", invoker = Raws.realkc) {
            assertReply("You need the following permission to perform that action: **Kick Members**")
        }
    }

    @Test
    fun testBotUnprivileged() {
        SentinelState.setRoles(member = Raws.self, roles = emptyList())
        testCommand(message = ";;kick realkc Generic shitposting", invoker = Raws.napster) {
            assertReplyContains("I need the following permission to perform that action: **Kick Members**")
        }
    }

    @Test
    fun testOwnerTargetFail() {
        testCommand(message = ";;kick fre_d", invoker = Raws.napster) {
            assertReplyContains(i18n("kickFailOwner"))
        }
    }

    @Test
    fun testSelfFail() {
        testCommand(message = ";;kick napster", invoker = Raws.napster) {
            assertReplyContains(i18n("kickFailSelf"))
        }
    }

    @Test
    fun testMyselfFail() {
        testCommand(message = ";;kick fredboat", invoker = Raws.napster) {
            assertReplyContains(i18n("kickFailMyself"))
        }
    }

    /* Hierachy fails */

    @Test
    fun testSameRoleFail() {
        SentinelState.setRoles(member = Raws.realkc, roles = listOf(Raws.adminRole.id))
        testCommand(message = ";;kick realkc", invoker = Raws.napster) {
            assertReplyContains(i18nFormat("modFailUserHierarchy", Raws.realkc.nickname!!))
        }
    }

    @Test
    fun testBotSameRoleFail() {
        SentinelState.setRoles(member = Raws.self, roles = listOf(Raws.adminRole.id))
        SentinelState.setRoles(member = Raws.realkc, roles = listOf(Raws.adminRole.id))
        SentinelState.setRoles(member = Raws.napster, roles = listOf(Raws.uberAdminRole.id))
        testCommand(message = ";;kick realkc", invoker = Raws.napster) {
            assertReplyContains(i18nFormat("modFailBotHierarchy", Raws.realkc.nickname!!))
        }
    }

}