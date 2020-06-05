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

package fredboat.command.info

import com.fredboat.sentinel.entities.Embed
import com.fredboat.sentinel.entities.coloredEmbed
import com.fredboat.sentinel.entities.field
import fredboat.command.music.control.DestroyCommand
import fredboat.commandmeta.CommandInitializer
import fredboat.commandmeta.CommandRegistry
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.commandmeta.abs.IInfoCommand
import fredboat.definitions.Module
import fredboat.definitions.PermissionLevel
import fredboat.messaging.internal.Context
import fredboat.perms.PermsUtil
import fredboat.util.TextUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.streams.toList

/**
 * Created by napster on 22.03.17.
 *
 * YO DAWG I HEARD YOU LIKE COMMANDS SO I PUT
 * THIS COMMAND IN YO BOT SO YOU CAN SHOW MORE
 * COMMANDS WHILE YOU EXECUTE THIS COMMAND
 *
 * Display available commands
 */
class CommandsCommand(name: String, vararg aliases: String) : Command(name, *aliases), IInfoCommand {

    override suspend fun invoke(context: CommandContext) {
        if (!context.hasArguments()) {

            val enabledModules = context.enabledModules.toMutableList()
            if (!PermsUtil.checkPerms(PermissionLevel.BOT_ADMIN, context.member)) {
                enabledModules.remove(Module.ADMIN)// Don't show admin commands/modules for non admins
            }

            val prefixAndCommand = "`" + context.prefix + CommandInitializer.COMMANDS_COMM_NAME
            val translatedModuleNames = enabledModules.stream()
                    .map { module -> context.i18n(module.translationKey) }
                    .toList()
            context.reply(context.i18nFormat("modulesCommands", "$prefixAndCommand <module>`", "$prefixAndCommand $ALL`")
                    + "\n\n" + context.i18nFormat("musicCommandsPromotion", "`" + context.prefix + CommandInitializer.MUSICHELP_COMM_NAME + "`")
                    + "\n\n" + context.i18n("modulesEnabledInGuild") + " **" + translatedModuleNames.joinToString("**, **") + "**"
                    + "\n" + context.i18nFormat("commandsModulesHint", "`" + context.prefix + CommandInitializer.MODULES_COMM_NAME + "`"))
            return
        }

        val showHelpFor: MutableList<Module>
        if (context.rawArgs.toLowerCase().contains(ALL.toLowerCase())) {
            showHelpFor = ArrayList(Arrays.asList(*Module.values()))
            if (!PermsUtil.checkPerms(PermissionLevel.BOT_ADMIN, context.member)) {
                showHelpFor.remove(Module.ADMIN)//dont show admin commands/modules for non admins
            }
        } else {
            val module = CommandRegistry.whichModule(context.rawArgs, context)
            if (module == null) {
                context.reply(context.i18nFormat("moduleCantParse",
                        "`" + context.prefix + context.command.name) + "`")
                return
            } else {
                showHelpFor = mutableListOf(module)
            }
        }

        val embed = coloredEmbed {
            showHelpFor.forEach { module ->
                addModuleCommands(context, CommandRegistry.getCommandModule(module))
            }

            field {
                body = context.i18nFormat(
                        "commandsMoreHelp",
                        "`" + TextUtils.escapeMarkdown(context.prefix) + CommandInitializer.HELP_COMM_NAME + " <command>`"
                )
            }
        }
        context.reply(embed)
    }

    private suspend fun Embed.addModuleCommands(context: CommandContext, module: CommandRegistry) {
        val invokerPermissionLevel = PermsUtil.getPerms(context.member)
        val commands = module.deduplicatedCommands
                .stream()
                //do not show BOT_ADMIN or BOT_OWNER commands to users lower than that
                .filter { command ->
                    if (command is ICommandRestricted) {
                        val isUserAdmin = invokerPermissionLevel.level >= PermissionLevel.BOT_ADMIN.level
                        val isAdminCommand = command.minimumPerms.level >= PermissionLevel.BOT_ADMIN.level

                        return@filter isUserAdmin || !isAdminCommand
                    }
                    true
                }
                .toList()

        val prefix = context.prefix

        if (commands.size >= 6) {
            //split the commands into three even columns
            val sbs = arrayOfNulls<StringBuilder>(3)
            sbs[0] = StringBuilder()
            sbs[1] = StringBuilder()
            sbs[2] = StringBuilder()
            var i = 0
            for (c in commands) {
                if (c is DestroyCommand) {
                    continue//dont want to publicly show this one
                }
                sbs[i++ % 3]!!.append(prefix).append(c.name).append("\n")
            }

            field {
                title = context.i18n(module.module.translationKey)
                body = sbs[0].toString()
                inline = true
            }
            field {
                body = sbs[1].toString()
                inline = true
            }
            field {
                body = sbs[2].toString()
                inline = true
            }
        } else {
            val sb = StringBuilder()
            for (c in commands) {
                sb.append(prefix).append(c.name).append("\n")
            }

            field {
                title = context.i18n(module.module.translationKey)
                body = sb.toString()
                inline = true
            }
            field { inline = true }
            field { inline = true }
        }
    }

    override fun help(context: Context): String {
        return "{0}{1} <module> OR {0}{1} " + ALL + "\n#" + context.i18n("helpCommandsCommand")
    }

    companion object {
        private const val ALL = "all"
        private val log: Logger = LoggerFactory.getLogger(CommandsCommand::class.java)
    }
}
