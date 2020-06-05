/*
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package fredboat.command.admin

import fredboat.audio.lavalink.SentinelLavalink
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.definitions.PermissionLevel
import fredboat.messaging.internal.Context
import fredboat.sentinel.Guild
import fredboat.sentinel.getGuild

class GetNodeCommand(name: String, vararg aliases: String) : Command(name, *aliases), ICommandRestricted {

    override val minimumPerms: PermissionLevel
        get() = PermissionLevel.BOT_ADMIN

    override suspend fun invoke(context: CommandContext) {
        var guild: Guild? = null
        if (context.hasArguments()) {
            try {
                guild = getGuild(context.args[0].toLong())
            } catch (ignored: NumberFormatException) {
            }

            if (guild == null || !guild.selfPresent) {
                context.reply("Guild not found for id " + context.args[0])
                return
            }
        } else {
            guild = context.guild
        }
        val node = SentinelLavalink.INSTANCE.getLink(guild).node

        val reply = String.format("Guild %s id `%s` lavalink socket: `%s`",
                context.guild.name, context.guild.id, node.toString())

        //sensitive info, send it by DM
        context.replyPrivateMono(reply)
                .doOnError {
                    context.replyWithName("Could not DM you, adjust your privacy settings.")
                }.subscribe {
                    context.replyWithName("Sent you a DM with the data.")
                }
    }

    override fun help(context: Context): String {
        return "{0}{1}\n#Show information about the currently assigned lavalink node of this guild."
    }
}
