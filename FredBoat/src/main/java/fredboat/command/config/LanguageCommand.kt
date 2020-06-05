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

package fredboat.command.config

import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IConfigCommand
import fredboat.config.property.AppConfig
import fredboat.definitions.PermissionLevel
import fredboat.feature.I18n
import fredboat.messaging.internal.Context
import fredboat.perms.PermsUtil
import fredboat.util.TextUtils
import fredboat.util.localMessageBuilder
import java.util.*

class LanguageCommand(name: String, vararg aliases: String) : Command(name, *aliases), IConfigCommand {

    override suspend fun invoke(context: CommandContext) {
        val guild = context.guild
        if (!context.hasArguments()) {
            handleNoArgs(context)
            return
        }

        if (!PermsUtil.checkPermsWithFeedback(PermissionLevel.ADMIN, context))
            return

        //Assume proper usage and that we are about to set a new language
        try {
            I18n.set(guild, context.args[0])
        } catch (e: I18n.LanguageNotSupportedException) {
            context.replyWithName(context.i18nFormat("langInvalidCode", context.args[0]))
            return
        }

        context.replyWithName(context.i18nFormat("langSuccess", I18n.getLocale(guild).nativeName))
    }

    private fun handleNoArgs(context: CommandContext) {
        val mb = localMessageBuilder()
                .append(context.i18n("langInfo").replace(AppConfig.DEFAULT_PREFIX, TextUtils.escapeMarkdown(context.prefix)))
                .append("\n\n")

        val keys = ArrayList(I18n.LANGS.keys)
        keys.sort()

        for (key in keys) {
            val loc = I18n.LANGS[key]!!
            mb.append("**`").append(loc.code).append("`** - ").append(loc.nativeName)
            mb.append("\n")
        }

        mb.append("\n")
        mb.append(context.i18n("langDisclaimer"))

        context.reply(mb.build())
    }

    override fun help(context: Context): String {
        return "{0}{1} OR {0}{1} <code>\n#" + context.i18n("helpLanguageCommand")
    }
}
