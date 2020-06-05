package fredboat.command.admin

import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.definitions.PermissionLevel
import fredboat.messaging.internal.Context
import fredboat.util.TextUtils

class GuildDebugCommand(name: String, vararg aliases: String) : Command(name, *aliases), ICommandRestricted {
    override val minimumPerms = PermissionLevel.BOT_ADMIN

    override suspend fun invoke(context: CommandContext) {
        val builder = StringBuilder()
        builder.append("Guild: ${context.guild}\n")
        builder.append("Routing key: ${context.routingKey}\n")

        builder.append("\nMembers:\n")
        context.guild.members.values.forEach {
            builder.append("  $it, roles: ${it.roles}\n")
        }

        builder.append("\nText channels:\n")
        context.guild.textChannels.values.forEach {
            builder.append("  $it, can write: ${it.canTalk()}\n")
        }

        builder.append("\nVoice channels:\n")
        context.guild.voiceChannels.values.forEach {
            builder.append("  $it, limit: ${it.userLimit}\n")
            it.members.forEach {
                builder.append("    $it\n")
            }
        }

        builder.append("\nRoles:\n")
        context.guild.roles.values.forEach {
            builder.append("  $it\n")
        }

        builder.append("\n")

        TextUtils.postToPasteService(builder.toString()).thenApply {
            if (it.isPresent) {
                context.replyPrivate(it.get())
            } else {
                context.reply("Failed to post to paste service")
            }
        }
    }

    override fun help(context: Context) = "Displays FredBoat state of this guild"
}