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

import fredboat.agent.GuildCacheInvalidationAgent
import fredboat.audio.player.PlayerRegistry
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.definitions.PermissionLevel
import fredboat.messaging.internal.Context
import fredboat.sentinel.InternalGuild

/**
 * @author Frederikam
 */
class UnsubCommand(
        name: String,
        vararg aliases: String,
        val playerRegistry: PlayerRegistry
) : Command(name, *aliases), ICommandRestricted {

    override val minimumPerms: PermissionLevel
        get() = PermissionLevel.BOT_ADMIN

    override suspend fun invoke(context: CommandContext) {
        playerRegistry.destroyPlayer(context.guild)
        GuildCacheInvalidationAgent.INSTANCE.invalidateGuild(context.guild as InternalGuild)
        context.reply("Unsubscribed from this guild.")
    }

    override fun help(context: Context): String {
        return "{0}{1} [code]\n#Unsubscribe from this guild."
    }
}
