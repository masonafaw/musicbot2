package fredboat.command.util


import fredboat.command.info.HelpCommand
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IUtilCommand
import fredboat.main.Launcher
import fredboat.messaging.internal.Context
import fredboat.util.DiscordUtil

class CalcShardCommand(name: String, vararg aliases: String) : Command(name, *aliases), IUtilCommand {
    override suspend fun invoke(context: CommandContext) {
        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context)
            return
        }

        val guildId = try {
            context.args[0].toLong()
        } catch (e: NumberFormatException) {
            HelpCommand.sendFormattedCommandHelp(context)
            return
        }
        
        val id = DiscordUtil.getShardId(guildId, Launcher.botController.appConfig)
        context.reply("Guild $guildId belongs to $id")
    }

    override fun help(context: Context): String {
        return "{0}{1} <guild-id>\n# Calculate the guild's shard ID"
    }

}