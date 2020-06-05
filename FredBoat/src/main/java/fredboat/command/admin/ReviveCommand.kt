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

import com.fredboat.sentinel.entities.ReviveShardRequest
import fredboat.command.info.HelpCommand
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.definitions.PermissionLevel
import fredboat.main.Launcher
import fredboat.messaging.internal.Context
import fredboat.util.DiscordUtil
import kotlinx.coroutines.reactive.awaitFirst

/**
 * @author frederik
 */
class ReviveCommand(name: String, vararg aliases: String) : Command(name, *aliases), ICommandRestricted {

    override val minimumPerms = PermissionLevel.BOT_ADMIN

    override suspend fun invoke(context: CommandContext) {
        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context)
            return
        }

        val shardId = try {
            if (context.args.size > 1 && context.args[0] == "guild") {
                val guildId = java.lang.Long.valueOf(context.args[1])
                DiscordUtil.getShardId(guildId, Launcher.botController.appConfig)
            } else {
                Integer.parseInt(context.args[0])
            }
        } catch (e: NumberFormatException) {
            HelpCommand.sendFormattedCommandHelp(context)
            return
        }

        val routingKey = context.sentinel.tracker.getKey(shardId)
        context.sentinel.send<String>(routingKey, ReviveShardRequest(shardId)).awaitFirst()
        context.replyWithName("Queued shard revive for shard $shardId")
        // TODO Launcher.getBotController().getShardManager().restart(shardId); // If not found it will just function like #start()
    }

    override fun help(context: Context): String {
        return "{0}{1} <shardId> OR {0}{1} guild <guildId>\n#Revive the specified shard, or the shard of the specified guild."
    }
}
