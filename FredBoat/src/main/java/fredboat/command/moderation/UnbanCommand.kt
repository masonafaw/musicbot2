/*
 *
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
import fredboat.commandmeta.abs.CommandContext
import fredboat.messaging.internal.Context
import fredboat.perms.Permission
import fredboat.sentinel.User
import fredboat.util.ArgumentUtil
import fredboat.util.TextUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.MessageDeliveryMode
import reactor.core.publisher.Mono

/**
 * Created by napster on 27.01.18.
 *
 * Unban a user.
 */
class UnbanCommand(name: String, vararg aliases: String) : DiscordModerationCommand(name, *aliases) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(UnbanCommand::class.java)
    }

    override fun modAction(args: DiscordModerationCommand.ModActionInfo): Mono<Unit> {
        return args.context.sentinel.genericMonoSendAndReceive<String, Unit>(
                exchange = SentinelExchanges.REQUESTS,
                request = ModRequest(
                        guildId = args.context.guild.id,
                        userId = args.targetUser.id,
                        type = ModRequestType.UNBAN,
                        reason = args.formattedReason
                ),
                routingKey = args.context.routingKey,
                mayBeEmpty = true,
                deliveryMode = MessageDeliveryMode.PERSISTENT,
                transform = {}
        )
    }

    override fun requiresMember() = false

    override fun onSuccess(args: DiscordModerationCommand.ModActionInfo): () -> Unit {
        val successOutput = args.context.i18nFormat("unbanSuccess",
                args.targetUser.asMention + " " + TextUtils.escapeAndDefuse(args.targetAsString()))

        return {
            args.context.replyWithName(successOutput)
        }
    }

    override fun onFail(args: DiscordModerationCommand.ModActionInfo): (t: Throwable) -> Unit {
        val escapedTargetName = TextUtils.escapeAndDefuse(args.targetAsString())
        return { t ->
            val context = args.context
            if (t is IllegalArgumentException) { //user was not banned in this guild (see GuildController#unban(String) handling of the response)
                context.reply(context.i18nFormat("modUnbanFailNotBanned", args.targetUser.asMention))
            } else {
                log.error("Failed to unban ${args.targetUser} from ${args.context.guild}")
                context.replyWithName(context.i18nFormat("modUnbanFail",
                        args.targetUser.asMention + " " + escapedTargetName))
            }
        }
    }

    override suspend fun fromFuzzySearch(context: CommandContext, searchTerm: String): Mono<User> {
        return fetchBanlist(context)
                .collectList()
                .map { banlist ->
            ArgumentUtil.checkSingleFuzzyUserSearchResult(
                    banlist.map { User(it.user) }, context, searchTerm, true).orElse(null)
        }
    }

    override suspend fun checkPreconditionWithFeedback(user: User, context: CommandContext): Mono<Boolean> {
        return fetchBanlist(context)
                .collectList()
                .map { banlist ->
                    if (banlist.stream().noneMatch { it.user.id == user.id }) {
                        context.reply(context.i18nFormat("modUnbanFailNotBanned", user.asMention))
                        return@map false
                    }
                    true
                }
    }

    override suspend fun checkAuthorizationWithFeedback(args: DiscordModerationCommand.ModActionInfo): Boolean {
        return args.context.run {
            checkInvokerPermissionsWithFeedback(Permission.BAN_MEMBERS)
                    && checkSelfPermissionsWithFeedback(Permission.BAN_MEMBERS)
        }
    }

    //don't dm users upon being unbanned
    override fun dmForTarget(args: DiscordModerationCommand.ModActionInfo): String? = null

    override fun help(context: Context): String {
        return ("{0}{1} userId OR username\n"
                + "#" + context.i18n("helpUnbanCommand"))
    }
}
