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

import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.definitions.PermissionLevel
import fredboat.main.Launcher
import fredboat.messaging.internal.Context
import fredboat.shared.constant.ExitCodes
import fredboat.util.TextUtils
import kotlinx.coroutines.reactive.awaitFirst
import java.time.Duration

/**
 * @author frederik
 */
class ExitCommand(name: String, vararg aliases: String) : Command(name, *aliases), ICommandRestricted {

    private var code = TextUtils.randomAlphaNumericString(4)

    override val minimumPerms: PermissionLevel
        get() = PermissionLevel.BOT_ADMIN

    override suspend fun invoke(context: CommandContext) {
        if (context.hasArguments()) {
            if (context.rawArgs == code) {
                try {
                    context.replyWithNameMono(":wave:")
                            .timeout(Duration.ofSeconds(5))
                            .awaitFirst()
                } catch (ignored: Exception) {
                }

                Launcher.botController.shutdownHandler.shutdown(ExitCodes.EXIT_CODE_NORMAL)
                return
            } else {
                context.reply(String.format("Your input `%s` did not fit the required code `%s`. A new code will be issued.",
                        TextUtils.escapeMarkdown(context.rawArgs), code))
            }
        }
        code = TextUtils.randomAlphaNumericString(4)
        context.reply(String.format("This will **shut down the whole bot**. " + "Please confirm by issuing this command again, with the following confirmation code appended: `%s`", code))
    }

    override fun help(context: Context): String {
        return "{0}{1} [code]\n#Shut down the bot."
    }
}
