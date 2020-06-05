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

import com.fredboat.sentinel.entities.Ban
import fredboat.command.info.HelpCommand
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IModerationCommand
import fredboat.definitions.PermissionLevel
import fredboat.feature.I18n
import fredboat.main.getBotController
import fredboat.perms.Permission
import fredboat.sentinel.Guild
import fredboat.sentinel.Member
import fredboat.sentinel.Sentinel
import fredboat.sentinel.User
import fredboat.shared.constant.DistributionEnum
import fredboat.util.ArgumentUtil
import fredboat.util.TextUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import reactor.core.publisher.toMono
import java.text.MessageFormat
import java.util.regex.Pattern
import javax.annotation.CheckReturnValue

/**
 * Created by napster on 27.01.18.
 *
 * Base class for Kick, Ban, Softban commands, and possibly others
 *
 * Conventions for these are as follows:
 * ;;command @memberMention audit reason with whitespace allowed
 * or
 * ;;command memberNameThatWillBeFuzzySearched audit reason with whitespace allowed
 * or
 * ;;command id snowflakeUserId audit reason with whitespace allowed
 */
abstract class DiscordModerationCommand protected constructor(name: String, vararg aliases: String)
    : Command(name, *aliases), IModerationCommand {

    companion object {

        private val log = LoggerFactory.getLogger(DiscordModerationCommand::class.java)

        private const val AUDIT_LOG_MAX_LENGTH = 512 // 512 is a hard limit by discord
        const val DEFAULT_DELETE_DAYS = 1    // same as the 24hours default in the discord client

        // https://regex101.com/r/7ss0ho/1
        // group 1: userid from a mention at the left side of the input string
        // group 2: rest of the command (right side of input string), can be empty
        private val USER_MENTION = Pattern.compile("^<@!?([0-9]+)>(.*)$")

        // https://regex101.com/r/f6uIGA/1
        // group 1: userid a raw number with atleast 15 digits at the left side of the input string
        // group 2: rest of the command (right side of input string), can be empty
        private val USER_ID = Pattern.compile("^([0-9]{15,})(.*)$")

        protected fun formatReasonForAuditLog(plainReason: String, invoker: Member, target: User): String {
            val i18nAuditLogMessage = MessageFormat.format(
                    I18n.get(invoker.guild).getString("modAuditLogMessage"),
                    TextUtils.asString(invoker),
                    TextUtils.asString(target)
            ) + ", "
            val leftoverSpace = AUDIT_LOG_MAX_LENGTH - i18nAuditLogMessage.length
            return i18nAuditLogMessage + if (plainReason.length > leftoverSpace)
                plainReason.substring(0, leftoverSpace)
            else
                plainReason
        }
    }

    /**
     * Returns the [Mono] to be issued by this command
     */
    protected abstract fun modAction(args: ModActionInfo): Mono<Unit>

    /**
     * @return true if this mod action requires the target to be a member of the guild (example: kick). The invoker will
     * be informed about this requirement if their input does not yield a member of the guild
     */
    protected abstract fun requiresMember(): Boolean

    /**
     * Returns the success handler for the mod action
     */
    protected abstract fun onSuccess(args: ModActionInfo): () -> Unit

    /**
     * Returns the failure handler for the mod action
     */
    protected abstract fun onFail(args: ModActionInfo): (t: Throwable) -> Unit

    /**
     * Checks ourself, the context.member, and the target member for proper authorization, and replies with appropriate
     * feedback if any check fails. Some guild specific checks are only run if the target member is not null, assuming
     * that in that case a mod action is being performed on a user that is not a member of the guild (for example,
     * banning a user that left the guild)
     *
     * @return true if all checks are successful, false otherwise. In case the return value is false, the invoker has
     * received feedback as to why.
     */
    protected abstract suspend fun checkAuthorizationWithFeedback(args: ModActionInfo): Boolean

    /**
     * Parse the context of the command into a ModActionInfo.
     * If that is not possible, reply a reason to the user, and return an empty Mono.
     *
     * Can be overridden for custom behaviour.
     *
     * @return a mono that may emit a ModActionInfo. Mono may be empty if something could not be parsed. In that case
     * the user will have been notified of the issue, no further action by the caller is necessary
     */
    protected open suspend fun parseModActionInfoWithFeedback(context: CommandContext): Mono<ModActionInfo> {
        //Ensure we have a search term
        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context)
            return Mono.empty()
        }

        //this variable is updated throughout the steps of the parsing with the remaining parts of the string
        var processed = context.rawArgs.trim { it <= ' ' }

        //parsing keep flag
        val keep: Boolean

        when {
            processed.endsWith("--keep") -> {
                keep = true
                processed = processed.substring(0, processed.length - "--keep".length)
            }
            processed.endsWith("-k") -> {
                keep = true
                processed = processed.substring(0, processed.length - "-k".length)
            }
            else -> keep = false
        }

        //parsing target and reason
        var targetId: Long = -1
        var reason = ""
        val mentionMatcher = USER_MENTION.matcher(processed)
        var userIdGroup = ""
        if (mentionMatcher.matches()) {
            userIdGroup = mentionMatcher.group(1)
            reason = mentionMatcher.group(2)
        }
        if (userIdGroup.isEmpty()) {
            val idMatcher = USER_ID.matcher(processed)
            if (idMatcher.matches()) {
                userIdGroup = idMatcher.group(1)
                reason = idMatcher.group(2)
            }
        }

        if (!userIdGroup.isEmpty()) {
            try {
                targetId = userIdGroup.toLong()
            } catch (e: NumberFormatException) {
                log.error("Failed to parse unsigned long from {}. Fix the regexes?", userIdGroup)
            }
        }

        var userMono: Mono<User>
        val finalTargetId = targetId
        if (targetId == -1L) {
            val search = context.args[0]
            reason = processed.replaceFirst(Pattern.quote(search).toRegex(), "").trim { it <= ' ' }
            userMono = fromFuzzySearch(context, search)
        } else {
            userMono = fetchUser(context.guild, targetId)
        }
        userMono = userMono.onErrorResume {
            if(targetId != -1L) {
                var reply = context.i18nFormat("parseNotAUser", "`$finalTargetId`")
                reply += "\n" + context.i18nFormat("parseSnowflakeIdHelp", HelpCommand.LINK_DISCORD_DOCS_IDS)
                context.reply(reply)
            }
            Mono.empty()
        }

        val finalReason = reason
        var user: User? = null
        userMono = userMono.doOnSuccess { user = it }
        return userMono.then(Mono.create { sink: MonoSink<ModActionInfo> ->
            if (user == null) {
                sink.success() // Unable to find user, already sent feedback
                return@create
            }
            GlobalScope.launch {
                checkPreconditionWithFeedback(user!!, context)
                        .doOnError { sink.error(it) }
                        .subscribe { preconditionMet ->
                            if (!preconditionMet) {
                                sink.success() // Already given feedback
                                return@subscribe
                            }

                            val guildMember = context.guild.getMember(user!!.id)
                            sink.success(ModActionInfo(context, guildMember, user!!, keep, finalReason))
                        }
            }
        })
    }

    /**
     * @param routingGuild Provides routing key and [Sentinel]
     * @param userId The user to fetch from discord.
     * @return A [Mono] that will complete with an optional [User]. The Mono will be empty if Discord doesn't know
     * any user by the [userId] that was passed to this method, otherwise if will contain a (possibly fake from JDAs point
     * of view) user.
     *
     * This method will log any unexpected exceptions, and return an empty Mono instead. Callers of this
     * don't need to handle throwables in the return completion stage.
     */
    @CheckReturnValue
    private fun fetchUser(routingGuild: Guild, userId: Long): Mono<User> {
        routingGuild.getMember(userId)?.let { return it.user.toMono() }
        return routingGuild.sentinel.getUser(userId, routingGuild.routingKey)
    }

    /**
     * @return Does a fuzzy search for a user. Default implementation fuzzy searches the guild of the context.
     */
    @CheckReturnValue
    protected open suspend fun fromFuzzySearch(context: CommandContext, searchTerm: String): Mono<User> =
            ArgumentUtil.checkSingleFuzzyMemberSearchResult(context, searchTerm, true)?.user?.toMono()
                    ?: Mono.empty()

    /**
     * Check preconditions of a command. Examples:
     * - To kick a user, they need to be a member of a guild.
     * - To unban a user, they need to be on the banlist.
     * If the precondition is not met, this method should give feedback to the invoker about the unmet precondition.
     */
    protected open suspend fun checkPreconditionWithFeedback(user: User, context: CommandContext): Mono<Boolean> {
        if (requiresMember() && !context.guild.isMember(user)) {
            context.reply(context.i18nFormat("parseNotAMember", user.asMention))
            return false.toMono()
        }

        return true.toMono()
    }

    override suspend fun invoke(context: CommandContext) {
        if (context.memberLevel() < PermissionLevel.BOT_ADMIN &&
                getBotController().appConfig.distribution != DistributionEnum.DEVELOPMENT) {
            if (this is UnbanCommand || this is SoftbanCommand) {
                context.reply("The unban and softban commands have been temporarily disabled. We have made significant" +
                        " changes to these, but have yet to thoroughly test that they are secure. We do not want to" +
                        " risk exploit of these commands.")
                return
            }
        }

        val modActionInfo = parseModActionInfoWithFeedback(context)
                .doOnError { t ->
                    TextUtils.handleException("Failed to parse a moderation action", t, context)
                }.awaitFirstOrNull() ?: return

        val hasAuthorization = checkAuthorizationWithFeedback(modActionInfo)

        if (!hasAuthorization) return // The invoker has been told

        val modAction = {
            modAction(modActionInfo)
                    .doOnError { onFail(modActionInfo)(it) }
                    .doOnSuccess { onSuccess(modActionInfo)() }
                    .log("action")
                    .subscribe()
        }

        // dm the target about the action. do this before executing the action.
        val dm = dmForTarget(modActionInfo)
        if (dm == null || modActionInfo.targetUser.isBot) {
            //no dm -> just do the mod action
            modAction()
            return
        }
        context.sentinel.sendPrivateMessage(modActionInfo.targetUser, dm)
                //run the mod action no matter if the dm fails or succeeds
                .doOnSuccessOrError { _, _ -> modAction() }
                .subscribe()
    }

    /**
     * Fetch the banlist of the guild of the context. If that fails, the context is informed about
     * the issue. Returns null when we do not have permission.
     */
    protected suspend fun fetchBanlist(context: CommandContext): Flux<Ban> {
        //need ban perms to read the ban list
        if (!context.checkSelfPermissionsWithFeedback(Permission.BAN_MEMBERS)) return Flux.empty()

        val guild = context.guild
        return guild.sentinel.getBanList(guild).doOnError {
            context.replyWithName(context.i18n("modBanlistFail"))
        }
    }

    /**
     * @return An optional message that will be attempted to be sent to the target user before executing the moderation
     * action against them, so that they know what happened to them.
     */
    protected abstract fun dmForTarget(args: ModActionInfo): String?


    //pass information between methods called inside this class and its subclasses
    protected class ModActionInfo(val context: CommandContext, val targetMember: Member?,
                                  /**
                                   * May be a fake user, so do not DM them.
                                   */
                                  val targetUser: User,
                                  val isKeepMessages: Boolean, rawReason: String?) {
        /**
         * Full reason, including a prefaced "Reason: ".
         * Do not use for audit log, use [ModActionInfo.formattedReason] instead.
         */
        val plainReason: String = context.i18n("modReason") + ": " +
                if (rawReason == null || rawReason.trim { it <= ' ' }.isEmpty())
                    context.i18n("modAudioLogNoReason")
                else
                    rawReason

        /**
         * Suitable for display discord audit logs. Will add information about who issued the ban, and respect Discords
         * length limit for audit log reasons.
         */
        val formattedReason: String
            get() = formatReasonForAuditLog(this.plainReason, this.context.member, this.targetUser)

        fun targetAsString(): String {
            return if (this.targetMember != null) {
                TextUtils.asString(this.targetMember)
            } else {
                TextUtils.asString(this.targetUser)
            }
        }
    }
}
