package fredboat.testutil

import fredboat.commandmeta.abs.CommandContext
import fredboat.sentinel.InternalGuild
import fredboat.sentinel.RawGuild
import fredboat.sentinel.RawMember
import fredboat.sentinel.RawTextChannel
import fredboat.testutil.extensions.DockerExtension
import fredboat.testutil.extensions.SharedSpringContext
import fredboat.testutil.sentinel.CommandTester
import fredboat.testutil.sentinel.Raws
import fredboat.util.ArgumentUtil
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(DockerExtension::class, SharedSpringContext::class)
open class IntegrationTest : BaseTest() {
    companion object {
        lateinit var commandTester: CommandTester // Set by SharedSpringContext
    }

    fun testCommand(
            message: String,
            invoker: RawMember = Raws.owner,
            guild: RawGuild = Raws.guild,
            channel: RawTextChannel = Raws.generalChannel,
            block: suspend CommandContext.() -> Unit
    ) {
        commandTester.testCommand(message, guild, channel, invoker, block)
    }

    fun testCommand(
            message: String,
            invoker: String,
            guild: RawGuild = Raws.guild,
            channel: RawTextChannel = Raws.generalChannel,
            block: suspend CommandContext.() -> Unit
    ) {
        val user = ArgumentUtil.fuzzyMemberSearch(InternalGuild(guild), invoker, true)[0]
        commandTester.testCommand(message, guild, channel, user.raw, block)
    }
}