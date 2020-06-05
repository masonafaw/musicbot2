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

package fredboat.command.util

import com.fredboat.sentinel.entities.embed
import com.fredboat.sentinel.entities.field
import fredboat.command.info.HelpCommand
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.messaging.internal.Context
import fredboat.perms.IPermissionSet
import fredboat.sentinel.Role
import fredboat.util.ArgumentUtil
import kotlinx.coroutines.reactive.awaitSingle
import java.awt.Color

/**
 * Created by napster on 18.01.18.
 */
class RoleInfoCommand(name: String, vararg aliases: String) : Command(name, *aliases) {

    companion object {
        private const val DEFAULT_COLOR_RAW = 0x1FFFFFFF
    }

    override suspend fun invoke(context: CommandContext) {

        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context)
            return
        }

        val roles = ArgumentUtil.fuzzyRoleSearch(context.guild, context.rawArgs)
        val role = ArgumentUtil.checkSingleFuzzySearchResult(roles, context, context.rawArgs) as Role? ?: return
        val roleInfo = role.info.awaitSingle()

        // Todo i18n
        val embed = embed {
            title = "Information about role " + role.name + ":"
            color = roleInfo.colorRgb

            field("Name", role.name, true)
            field("Id", role.idString, true)
            if (roleInfo.colorRgb != null) {
                field("Color", colorToHex(roleInfo.colorRgb!!), true)
            } else {
                field("Color", "❌", true)
            }
            field("Hoisted", roleInfo.isHoisted.toString(), true)
            field("Mentionable", roleInfo.isMentionable.toString(), true)
            field("Managed", roleInfo.isManaged.toString(), true)
            field("Permissions", permissionsToString(role.permissions), true)
        }

        context.reply(embed)
    }

    override fun help(context: Context): String {
        return "{0}{1} <role id/mention>\n#" + "Display information about a role." //todo i18n
    }

    private fun permissionsToString(perms: IPermissionSet): String {
        val list = perms.asList()
        return if (list.isEmpty()) {
            "none" //todo i18n
        } else list.joinToString(", ") { it.uiName }
    }

    private fun colorToHex(color: Int): String {
        if (color == DEFAULT_COLOR_RAW) {
            return "none" //todo i18n
        }
        val c = Color(color)
        return "#" +
                Integer.toString(c.red, 16).toUpperCase() +
                Integer.toString(c.green, 16).toUpperCase() +
                Integer.toString(c.blue, 16).toUpperCase()
    }
}
