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

import fredboat.command.info.HelpCommand
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IUtilCommand
import fredboat.messaging.internal.Context
import kotlinx.coroutines.reactive.awaitSingle

class AvatarCommand(name: String, vararg aliases: String) : Command(name, *aliases), IUtilCommand {

    override suspend fun invoke(context: CommandContext) {
        if (context.mentionedMembers.isEmpty()) {
            HelpCommand.sendFormattedCommandHelp(context)
        } else {
            val avatar = context.mentionedMembers[0].info.awaitSingle().iconUrl

            if (avatar != null) {
                context.replyWithName(context.i18nFormat("avatarSuccess", avatar))
            } else {
                context.replyWithName("That user has no avatar") // todo i18n
            }
        }
    }

    override fun help(context: Context): String {
        return "{0}{1} @<username>\n#" + context.i18n("helpAvatarCommand")
    }
}
