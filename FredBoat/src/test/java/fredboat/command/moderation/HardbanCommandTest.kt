package fredboat.command.moderation

import com.fredboat.sentinel.entities.ModRequestType
import fredboat.testutil.sentinel.Raws
import fredboat.testutil.sentinel.SentinelState
import fredboat.testutil.sentinel.assertReply
import fredboat.testutil.sentinel.assertReplyContains
import org.junit.jupiter.api.Test

class HardbanCommandTest : AbstractModerationTest() {

    @Test
    fun testWorking() {
        testCommand(message = ";;ban realkc Generic shitposting", invoker = Raws.napster) {
            expect(ModRequestType.BAN, Raws.realkc, "Generic shitposting")
        }
    }

    @Test
    fun testWorkingOwner() {
        testCommand(message = ";;ban napster Generic shitposting", invoker = Raws.owner) {
            expect(ModRequestType.BAN, Raws.napster, "Generic shitposting")
        }
    }

    @Test
    fun testUnprivileged() {
        testCommand(message = ";;ban napster Generic shitposting", invoker = Raws.realkc) {
            assertReply("You need the following permission to perform that action: **Ban Members**")
        }
    }

    @Test
    fun testBotUnprivileged() {
        SentinelState.setRoles(member = Raws.self, roles = emptyList())
        testCommand(message = ";;ban realkc Generic shitposting", invoker = Raws.napster) {
            assertReplyContains("I need the following permission to perform that action: **Ban Members**")
        }
    }

    @Test
    fun testOwnerTargetFail() {
        testCommand(message = ";;ban fre_d", invoker = Raws.napster) {
            assertReplyContains(i18n("hardbanFailOwner"))
        }
    }

    @Test
    fun testSelfFail() {
        testCommand(message = ";;ban napster", invoker = Raws.napster) {
            assertReplyContains(i18n("hardbanFailSelf"))
        }
    }

    @Test
    fun testMyselfFail() {
        testCommand(message = ";;ban fredboat", invoker = Raws.napster) {
            assertReplyContains(i18n("hardbanFailMyself"))
        }
    }

    /* Hierachy fails */

    @Test
    fun testSameRoleFail() {
        SentinelState.setRoles(member = Raws.realkc, roles = listOf(Raws.adminRole.id))
        testCommand(message = ";;ban realkc", invoker = Raws.napster) {
            assertReplyContains(i18nFormat("modFailUserHierarchy", Raws.realkc.nickname!!))
        }
    }

    @Test
    fun testBotSameRoleFail() {
        SentinelState.setRoles(member = Raws.self, roles = listOf(Raws.adminRole.id))
        SentinelState.setRoles(member = Raws.realkc, roles = listOf(Raws.adminRole.id))
        SentinelState.setRoles(member = Raws.napster, roles = listOf(Raws.uberAdminRole.id))
        testCommand(message = ";;ban realkc", invoker = Raws.napster) {
            assertReplyContains(i18nFormat("modFailBotHierarchy", Raws.realkc.nickname!!))
        }
    }

}