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
 */

package fredboat.command.moderation

import com.fredboat.sentinel.SentinelExchanges
import com.fredboat.sentinel.entities.ModRequest
import com.fredboat.sentinel.entities.ModRequestType
import fredboat.messaging.internal.Context
import fredboat.perms.Permission
import fredboat.util.DiscordUtil
import fredboat.util.TextUtils
import fredboat.util.extension.escapeAndDefuse
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.MessageDeliveryMode
import reactor.core.publisher.Mono

/**
 * Created by napster on 19.04.17.
 *
 * Ban a user.
 */
class HardbanCommand(name: String, vararg aliases: String) : DiscordModerationCommand(name, *aliases) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(HardbanCommand::class.java)
    }

    override fun modAction(args: DiscordModerationCommand.ModActionInfo): Mono<Unit> {
        val deleteDays = if(args.isKeepMessages) 0 else DiscordModerationCommand.DEFAULT_DELETE_DAYS
        return args.context.sentinel.genericMonoSendAndReceive<String, Unit>(
                exchange = SentinelExchanges.REQUESTS,
                request = ModRequest(
                        guildId = args.context.guild.id,
                        userId = args.targetUser.id,
                        type = ModRequestType.BAN,
                        banDeleteDays = deleteDays,
                        reason = args.formattedReason
                ),
                routingKey = args.context.routingKey,
                mayBeEmpty = true,
                deliveryMode = MessageDeliveryMode.PERSISTENT,
                transform = {}
        )
    }

    override fun requiresMember() = false

    override fun onSuccess(args: DiscordModerationCommand.ModActionInfo) = {
            val successOutput = (args.context.i18nFormat("hardbanSuccess",
                    args.targetUser.asMention + " " + TextUtils.escapeAndDefuse(args.targetAsString()))
                    + "\n" + TextUtils.escapeAndDefuse(args.plainReason))
            args.context.replyWithName(successOutput)
        }

    override fun onFail(args: DiscordModerationCommand.ModActionInfo): (t: Throwable) -> Unit {
        val escapedTargetName = TextUtils.escapeAndDefuse(args.targetAsString())
        return { t ->
            log.error("Failed to issue ban", t)
            args.context.replyWithName(args.context.i18nFormat("modBanFail", "${args.targetUser.asMention} $escapedTargetName"))
        }
    }

    override suspend fun checkAuthorizationWithFeedback(args: DiscordModerationCommand.ModActionInfo): Boolean {
        val context = args.context
        val target = args.targetMember
        val mod = context.member

        if (!context.checkInvokerPermissionsWithFeedback(Permission.BAN_MEMBERS)) return false
        if (!context.checkSelfPermissionsWithFeedback(Permission.BAN_MEMBERS)) return false
        if (target == null) return false

        if (mod == target) {
            context.replyWithName(context.i18n("hardbanFailSelf"))
            return false
        }

        if (target.isOwner()) {
            context.replyWithName(context.i18n("hardbanFailOwner"))
            return false
        }

        if (target == target.guild.selfMember) {
            context.replyWithName(context.i18n("hardbanFailMyself"))
            return false
        }

        val modRole = DiscordUtil.getHighestRolePosition(mod).awaitFirst()
        val targetRole = DiscordUtil.getHighestRolePosition(target).awaitFirst()
        val selfRole = DiscordUtil.getHighestRolePosition(mod.guild.selfMember).awaitFirst()

        if (modRole <= targetRole && !mod.isOwner()) {
            context.replyWithName(context.i18nFormat("modFailUserHierarchy", target.effectiveName.escapeAndDefuse()))
            return false
        }

        if (selfRole <= targetRole) {
            context.replyWithName(context.i18nFormat("modFailBotHierarchy", target.effectiveName.escapeAndDefuse()))
            return false
        }

        return true
    }

    override fun dmForTarget(args: DiscordModerationCommand.ModActionInfo): String? {
        return args.context.i18nFormat("modActionTargetDmBanned",
                "**${TextUtils.escapeMarkdown(args.context.guild.name)}**",
                TextUtils.asString(args.context.user))+ "\n" + args.plainReason
    }

    override fun help(context: Context): String {
        return ("{0}{1} <user>\n"
                + "{0}{1} <user> <reason>\n"
                + "{0}{1} <user> <reason> --keep\n"
                + "#" + context.i18n("helpHardbanCommand") + "\n" + context.i18nFormat("modKeepMessages", "--keep or -k"))
    }
}

