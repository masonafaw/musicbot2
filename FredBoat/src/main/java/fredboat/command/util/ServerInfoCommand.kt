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

package fredboat.command.util

import com.fredboat.sentinel.entities.coloredEmbed
import com.fredboat.sentinel.entities.field
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IUtilCommand
import fredboat.messaging.internal.Context
import fredboat.util.TextUtils
import kotlinx.coroutines.reactive.awaitFirst
import java.time.format.DateTimeFormatter

/**
 * Created by midgard/Chromaryu/knight-ryu12 on 17/01/18.
 */
class ServerInfoCommand(name: String, vararg aliases: String) : Command(name, *aliases), IUtilCommand {

    override suspend fun invoke(context: CommandContext) {
        val guild = context.guild
        val guildInfo = guild.info.awaitFirst()

        val dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy")
        val embed = coloredEmbed {
            title = context.i18nFormat("serverinfoTitle", guild.name)
            thumbnail = guildInfo.iconUrl

            field(context.i18n("serverinfoOnlineUsers"), guildInfo.onlineMembers.toString(), true)
            field(context.i18n("serverinfoTotalUsers"), guild.members.size.toString(), true)
            field(context.i18n("serverinfoRoles"), guild.roles.size.toString(), true)
            field(context.i18n("serverinfoText"), guild.textChannels.size.toString(), true)
            field(context.i18n("serverinfoVoice"), guild.voiceChannels.size.toString(), true)
            field(context.i18n("serverinfoCreationDate"), guild.creationTime.format(dtf), true)
            field(context.i18n("serverinfoGuildID"), guild.idString, true)
            field(context.i18n("serverinfoVLv"), guildInfo.verificationLevel, true)
            field(context.i18n("serverinfoOwner"), guild.owner?.asMention ?: "<none>", true)

            val maxFieldLength = 1024
            var prefix = TextUtils.shorten(TextUtils.escapeMarkdown(context.prefix), maxFieldLength)
            if (prefix.isEmpty()) {
                prefix = "No Prefix"
            }
            field(context.i18n("prefix"), prefix, true)
        }

        context.reply(embed)
    }

    override fun help(context: Context): String {
        return "{0}{1}\n#" + context.i18n("helpServerInfoCommand")
    }
}
