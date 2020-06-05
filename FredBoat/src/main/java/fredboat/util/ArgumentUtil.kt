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

package fredboat.util

import fredboat.commandmeta.abs.CommandContext
import fredboat.sentinel.*
import java.util.*
import javax.annotation.CheckReturnValue
import kotlin.streams.toList

object ArgumentUtil {

    private const val FUZZY_RESULT_LIMIT = 10

    /**
     * Search a collection of users for a search term.
     *
     * @param users       list of users to be searched
     * @param term        the term the user shall be searched for
     * @param includeBots false to exclude bots from results
     */
    @CheckReturnValue
    fun fuzzyUserSearch(users: Collection<User>, term: String, includeBots: Boolean): List<User> {
        val list = ArrayList<User>()

        val searchTerm = term.toLowerCase()

        for (user in users) {
            if ((user.name.toLowerCase() + "#" + user.discrim).contains(searchTerm) || searchTerm.contains(user.id.toString())) {

                if (!includeBots && user.isBot) continue
                list.add(user)
            }
        }

        return list
    }

    /**
     * Search a list of users for exactly one match. If there are no matches / multiple ones, the context will be
     * informed accordingly informed, so that the caller of this method has nothing to do and should return.
     *
     * @param users       list of users to be searched
     * @param context     the context of this search, where output may be directed if non / multiple results are found
     * @param term        the term the user shall be searched for
     * @param includeBots false to exclude bots from results
     */

    @CheckReturnValue
    fun checkSingleFuzzyUserSearchResult(users: Collection<User>, context: CommandContext, term: String, includeBots: Boolean): Optional<User> {
        val found = fuzzyUserSearch(users, term, includeBots)
        return when (found.size) {
            0 -> {
                context.reply(context.i18nFormat("fuzzyNothingFound", term))
                Optional.empty()
            }
            1 -> Optional.of(found[0])
            else -> {
                context.reply(context.i18n("fuzzyMultiple") + "\n"
                        + formatFuzzyUserResult(found, FUZZY_RESULT_LIMIT, 1900))
                Optional.empty()
            }
        }
    }

    @CheckReturnValue
    fun fuzzyMemberSearch(guild: Guild, term: String, includeBots: Boolean): List<Member> {
        @Suppress("NAME_SHADOWING")
        var term = term
        val list = ArrayList<Member>()

        term = term.toLowerCase()

        guild.members.forEach { _, mem ->
            if ((mem.name.toLowerCase() + "#" + mem.discrim).contains(term)
                    || mem.effectiveName.toLowerCase().contains(term)
                    || term.contains(mem.id.toString())) {

                if (!includeBots && mem.isBot) return@forEach
                list.add(mem)
            }
        }

        return list
    }

    @CheckReturnValue
    fun fuzzyRoleSearch(guild: Guild, term: String): List<Role> {
        @Suppress("NAME_SHADOWING")
        var term = term
        val list = ArrayList<Role>()

        term = term.toLowerCase()

        guild.roles.forEach { _, role ->
            if (role.name.toLowerCase().contains(term) || term.contains(role.id.toString())) {
                list.add(role)
            }
        }

        return list
    }

    @CheckReturnValue
    @JvmOverloads
    fun checkSingleFuzzyMemberSearchResult(context: CommandContext, term: String, includeBots: Boolean = false): Member? {
        val list = fuzzyMemberSearch(context.guild, term, includeBots)

        return when (list.size) {
            0 -> {
                context.reply(context.i18nFormat("fuzzyNothingFound", term))
                null
            }
            1 -> list[0]
            else -> {
                context.reply(context.i18n("fuzzyMultiple") + "\n"
                        + formatFuzzyMemberResult(list, FUZZY_RESULT_LIMIT, 1900))
                null
            }
        }
    }

    @CheckReturnValue
    fun formatFuzzyMemberResult(members: List<Member>, maxLines: Int, maxLength: Int): String {
        return formatFuzzyUserOrMemberResult(UserWithOptionalNick.fromMembers(members), maxLines, maxLength)
    }

    @CheckReturnValue
    fun formatFuzzyUserResult(users: List<User>, maxLines: Int, maxLength: Int): String {
        return formatFuzzyUserOrMemberResult(UserWithOptionalNick.fromUsers(users), maxLines, maxLength)
    }

    /**
     * Format a list of members as a text block, usually a result from a fuzzy search. The list should not be empty.
     *
     * @param maxLines  How many results to display. Pass Integer.MAX_VALUE to use as many as possible.
     * @param maxLength How many characters the resulting string may have as a max.
     */
    @CheckReturnValue
    fun formatFuzzyUserOrMemberResult(list: List<UserWithOptionalNick>, maxLines: Int, maxLength: Int): String {

        val toDisplay: List<UserWithOptionalNick>
        var addDots = false
        if (list.size > maxLines) {
            addDots = true
            toDisplay = list.subList(0, maxLines)
        } else {
            toDisplay = ArrayList(list)
        }

        val idPadding = toDisplay.stream()
                .mapToInt { member -> member.user.id.toString().length }
                .max()
                .orElse(0)
        val namePadding = toDisplay.stream()
                .mapToInt { member -> TextUtils.escapeBackticks(member.user.name).length }
                .max()
                .orElse(0) + 5//for displaying discrim
        val nickPadding = toDisplay.stream()
                .mapToInt { member -> member.getNick().map<Int>({ it.length }).orElse(0) }
                .max()
                .orElse(0)

        val lines = toDisplay.stream()
                .map { member ->
                    (TextUtils.padWithSpaces(member.user.id.toString(), idPadding, true)
                            + " " + TextUtils.padWithSpaces(TextUtils.escapeBackticks(member.user.name)
                            + "#" + member.user.discrim, namePadding, false)
                            + " " + TextUtils.escapeBackticks(TextUtils.padWithSpaces(member.getNick().orElse(""), nickPadding, false))
                            + "\n")
                }
                .toList()


        val sb = StringBuilder(TextUtils.padWithSpaces("Id", idPadding + 1, false)
                + TextUtils.padWithSpaces("Name", namePadding + 1, false)
                + TextUtils.padWithSpaces("Nick", nickPadding + 1, false) + "\n")

        val textBlockLength = 8
        val dotdotdot = "[...]"
        for (i in toDisplay.indices) {
            val line = lines[i]
            if (sb.length + line.length + dotdotdot.length + textBlockLength < maxLength) {
                sb.append(line)
            } else {
                sb.append(dotdotdot)
                addDots = false //already added
                break
            }
        }
        if (addDots) {
            sb.append(dotdotdot)
        }

        return TextUtils.asCodeBlock(sb.toString())
    }

    /**
     * Processes a list of mentionables (roles / users).
     * Replies in the context of there are none / more than one mentionable and returns null, otherwise returns the
     * single mentionable.
     */
    @CheckReturnValue
    fun checkSingleFuzzySearchResult(list: List<IMentionable>,
                                     context: CommandContext, term: String): IMentionable? {
        when (list.size) {
            0 -> {
                context.reply(context.i18nFormat("fuzzyNothingFound", term))
                return null
            }
            1 -> return list[0]
            else -> {
                val searchResults = StringBuilder()
                var i = 0
                for (mentionable in list) {
                    if (i == FUZZY_RESULT_LIMIT) break

                    when (mentionable) {
                        is Member -> searchResults.append("\nUSER ")
                                .append(mentionable.id)
                                .append(" ")
                                .append(mentionable.effectiveName)
                        is Role -> {
                            searchResults.append("\nROLE ")
                                    .append(mentionable.id)
                                    .append(" ")
                                    .append(mentionable.name)
                        }
                        else -> throw IllegalArgumentException("Expected Role or Member, got $mentionable")
                    }
                    i++
                }

                if (list.size > FUZZY_RESULT_LIMIT) {
                    searchResults.append("\n[...]")
                }

                context.reply(context.i18n("fuzzyMultiple") + "\n"
                        + TextUtils.asCodeBlock(searchResults.toString()))
                return null
            }
        }
    }

    /**
     * Helper method to combine all the options from command into a single String.
     * Will call trim on each combine.
     *
     * @param args Command arguments.
     * @return String object of the combined args or empty string.
     */
    fun combineArgs(args: Array<String>): String {
        val sb = StringBuilder()
        for (arg in args) {
            sb.append(arg.trim { it <= ' ' })
        }
        return sb.toString().trim { it <= ' ' }
    }

    class UserWithOptionalNick {

        val user: User
        private val nick: String?

        constructor(user: User) {
            this.user = user
            this.nick = null
        }

        constructor(member: Member) {
            this.user = member.user
            this.nick = member.nickname
        }

        fun getNick(): Optional<String> {
            return Optional.ofNullable(nick)
        }

        companion object {

            fun fromUsers(users: Collection<User>): List<UserWithOptionalNick> {
                return users.stream().map({ UserWithOptionalNick(it) }).toList()
            }

            fun fromMembers(members: Collection<Member>): List<UserWithOptionalNick> {
                return members.stream().map({ UserWithOptionalNick(it) }).toList()
            }
        }
    }
}
