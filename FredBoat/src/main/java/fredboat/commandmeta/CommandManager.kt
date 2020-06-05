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

package fredboat.commandmeta


import fredboat.audio.player.MusicTextChannelProvider
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.commandmeta.abs.IMusicCommand
import fredboat.definitions.PermissionLevel
import fredboat.feature.PatronageChecker
import fredboat.feature.metrics.Metrics
import fredboat.feature.togglz.FeatureFlags
import fredboat.perms.PermsUtil
import fredboat.sentinel.RawUser
import fredboat.shared.constant.BotConstants
import fredboat.util.DiscordUtil
import fredboat.util.TextUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@Component
class CommandManager(private val patronageChecker: PatronageChecker, private val musicTextChannelProvider: MusicTextChannelProvider,
                     @param:Qualifier("selfUser")
                     private val selfUser: RawUser) {

    companion object {
        val disabledCommands: Set<Command> = HashSet(0)
        val totalCommandsExecuted = AtomicInteger(0)
    }

    suspend fun prefixCalled(context: CommandContext) {
        val guild = context.guild
        val invoked = context.command
        val channel = context.textChannel
        val invoker = context.member

        totalCommandsExecuted.incrementAndGet()
        Metrics.commandsExecuted.labels(invoked.javaClass.simpleName).inc()

        if (FeatureFlags.PATRON_VALIDATION.isActive) {
            val status = patronageChecker.getStatus(guild)
            if (!status.isValid) {
                var msg = ("Access denied. This bot can only be used if invited from <https://patron.fredboat.com/> "
                        + "by someone who currently has a valid pledge on Patreon.\n**Denial reason:** " + status.reason + "\n\n")

                msg += "Do you believe this to be a mistake? If so reach out to Fre_d on Patreon <" + BotConstants.PATREON_CAMPAIGN_URL + ">"

                context.reply(msg)
                return
            }
        }

        //Hardcode music commands in FredBoatHangout. Blacklist any channel that isn't #spam_and_music or #staff, but whitelist Admins
        if (guild.id == BotConstants.FREDBOAT_HANGOUT_ID && DiscordUtil.isOfficialBot(selfUser.id)) {
            if (channel.id != 174821093633294338L // #spam_and_music
                    && channel.id != 217526705298866177L // #staff
                    && invoker.roles.none { it.id == BotConstants.FBH_MODERATOR_ROLE_ID }
                    && !PermsUtil.checkPerms(PermissionLevel.ADMIN, invoker)) {
                context.deleteMessage()
                val response = context.replyWithNameMono(
                        "Please read <#219483023257763842> for server rules and only use commands in <#174821093633294338>!"
                ).awaitSingle()
                delay(5000L)
                channel.deleteMessage(response.messageId).subscribe()
                return
            }
        }

        if (disabledCommands.contains(invoked)) {
            context.replyWithName("Sorry the `" + context.command.name + "` command is currently disabled. Please try again later")
            return
        }

        if (invoked is ICommandRestricted) {
            if (!PermsUtil.checkPermsWithFeedback((invoked as ICommandRestricted).minimumPerms, context)) {
                return
            }
        }

        if (invoked is IMusicCommand) {
            musicTextChannelProvider.setMusicChannel(channel)
        }

        try {
            invoked(context)
        } catch (e: Exception) {
            TextUtils.handleException("Caught exception while executing a command", e, context)
        }

    }
}
