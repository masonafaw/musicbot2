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
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.MessageDeliveryMode
import reactor.core.publisher.Mono

/**
 * Created by napster on 19.04.17.
 *
 *
 * Kick a user.
 */
class KickCommand(name: String, vararg aliases: String) : DiscordModerationCommand(name, *aliases) {

    override fun modAction(args: DiscordModerationCommand.ModActionInfo): Mono<Unit> {
        if (args.targetMember == null) {
            //we should not reach this place due to requiresMember() = true implementation
            log.warn("Requested kick action for null member. This looks like a bug. Returning Mono. User input:\n{}",
                    args.context.msg.content)
            return Mono.empty()
        }

        return args.context.sentinel.genericMonoSendAndReceive<String, Unit>(
                exchange = SentinelExchanges.REQUESTS,
                request = ModRequest(
                        guildId = args.context.guild.id,
                        userId = args.targetUser.id,
                        type = ModRequestType.KICK,
                        reason = args.formattedReason
                ),
                routingKey = args.context.routingKey,
                mayBeEmpty = true,
                deliveryMode = MessageDeliveryMode.PERSISTENT,
                transform = {}
        )
    }

    override fun requiresMember(): Boolean {
        return true //can't kick non-members
    }

    override fun onSuccess(args: DiscordModerationCommand.ModActionInfo): () -> Unit {
        val successOutput = (args.context.i18nFormat("kickSuccess",
                args.targetUser.asMention + " " + TextUtils.escapeAndDefuse(args.targetAsString()))
                + "\n" + TextUtils.escapeAndDefuse(args.plainReason))
        return {
            args.context.replyWithName(successOutput)
        }
    }

    override fun onFail(args: DiscordModerationCommand.ModActionInfo): (t: Throwable) -> Unit {
        val escapedTargetName = TextUtils.escapeAndDefuse(args.targetAsString())
        return { t ->
            val context = args.context
            log.error("Failed to kick user", t)
            context.replyWithName(context.i18nFormat("kickFail", 
                    args.targetUser.asMention + " " + escapedTargetName))
        }
    }

    override suspend fun checkAuthorizationWithFeedback(args: DiscordModerationCommand.ModActionInfo): Boolean {
        val context = args.context
        val target = args.targetMember
        val mod = context.member

        if (!context.checkInvokerPermissionsWithFeedback(Permission.KICK_MEMBERS)) return false
        if (!context.checkSelfPermissionsWithFeedback(Permission.KICK_MEMBERS)) return false
        if (target == null) return true

        if (mod == target) {
            context.replyWithName(context.i18n("kickFailSelf"))
            return false
        }

        if (target.isOwner()) {
            context.replyWithName(context.i18n("kickFailOwner"))
            return false
        }

        if (target == target.guild.selfMember) {
            context.replyWithName(context.i18n("kickFailMyself"))
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
        return args.context.i18nFormat("modActionTargetDmKicked",
                "**${TextUtils.escapeMarkdown(args.context.guild.name)}**",
                TextUtils.asString(args.context.user)) + "\n" + args.plainReason
    }

    override fun help(context: Context): String {
        return ("{0}{1} <user>\n"
                + "{0}{1} <user> <reason>\n"
                + "#" + context.i18n("helpKickCommand"))
    }

    companion object {

        private val log = LoggerFactory.getLogger(KickCommand::class.java)
    }
}
