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

import fredboat.audio.lavalink.SentinelLavalink
import fredboat.command.info.HelpCommand
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.definitions.PermissionLevel
import fredboat.messaging.internal.Context
import fredboat.perms.PermsUtil
import fredboat.util.TextUtils
import lavalink.client.io.LavalinkLoadBalancer
import java.net.URI
import java.net.URISyntaxException
import kotlin.streams.toList

class NodeAdminCommand(name: String, vararg aliases: String) : Command(name, *aliases), ICommandRestricted {

    override val minimumPerms: PermissionLevel
        get() = PermissionLevel.BOT_ADMIN

    override suspend fun invoke(context: CommandContext) {
        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context)
            return
        }
        when (context.args[0]) {
            "del", "delete", "remove", "rem", "rm" -> if (context.args.size < 2) {
                HelpCommand.sendFormattedCommandHelp(context)
            } else {
                remove(context)
            }
            "add" -> if (context.args.size < 4) {
                HelpCommand.sendFormattedCommandHelp(context)
            } else {
                add(context)
            }
            "show" -> if (context.args.size < 2) {
                HelpCommand.sendFormattedCommandHelp(context)
            } else {
                show(context)
            }
            "list" -> list(context)
            else -> list(context)
        }
    }

    private fun remove(context: CommandContext) {
        val name = context.args[1]
        val nodes = SentinelLavalink.INSTANCE.nodes
        var key = -1
        for (i in nodes.indices) {
            val node = nodes[i]
            if (node.name == name) {
                key = i
            }
        }
        if (key < 0) {
            context.reply("No node with name $name found.")
            return
        }

        SentinelLavalink.INSTANCE.removeNode(key)
        context.reply("Removed node $name")
    }

    private fun add(context: CommandContext) {
        val name = context.args[1]
        val uri: URI
        try {
            uri = URI(context.args[2])
        } catch (e: URISyntaxException) {
            context.reply(context.args[2] + " is not a valid URI")
            return
        }

        val password = context.args[3]
        SentinelLavalink.INSTANCE.addNode(name, uri, password)
        context.reply("Added node: " + name + " @ " + uri.toString())
    }

    private suspend fun show(context: CommandContext) {
        val name = context.args[1]
        val nodes = SentinelLavalink.INSTANCE.nodes.stream()
                .filter { ll -> ll.name == name }
                .toList()

        if (nodes.isEmpty()) {
            context.reply("No such node: $name, showing a list of all nodes instead")
            list(context)
            return
        }


        val stats = nodes[0].stats
        var out = "No stats have been received from this node! Is the node down?"
        if (stats != null) {
            out = TextUtils.asCodeBlock(stats.asJson.toString(4), "json")
        }
        context.reply(out)
    }

    private suspend fun list(context: CommandContext) {
        val lavalink = SentinelLavalink.INSTANCE


        var showHosts = false
        if (context.hasArguments() && context.rawArgs.contains("host")) {
            if (PermsUtil.checkPermsWithFeedback(PermissionLevel.BOT_ADMIN, context)) {
                showHosts = true
            } else {
                return
            }
        }

        val nodes = lavalink.nodes
        if (nodes.isEmpty()) {
            context.replyWithName("There are no remote lavalink nodes registered.")
            return
        }

        val messages = mutableListOf<String>()

        nodes.forEach { socket ->
            val stats = socket.stats
            var str = "Name:                " + socket.name + "\n"

            if (showHosts) {
                str += "Host:                    " + socket.remoteUri + "\n"
            }

            if (stats == null) {
                str += "No stats have been received from this node! Is the node down?"
                str += "\n\n"
                messages.add(str)
                return@forEach
            }

            str += "Playing players:         " + stats.playingPlayers + "\n"
            str += "Lavalink load:           " + TextUtils.formatPercent(stats.lavalinkLoad) + "\n"
            str += "System load:             " + TextUtils.formatPercent(stats.systemLoad) + " \n"
            str += "Memory:                  " + stats.memUsed / 1000000 + "MB/" + stats.memReservable / 1000000 + "MB\n"
            str += "---------------\n"
            str += "Average frames sent:     " + stats.avgFramesSentPerMinute + "\n"
            str += "Average frames nulled:   " + stats.avgFramesNulledPerMinute + "\n"
            str += "Average frames deficit:  " + stats.avgFramesDeficitPerMinute + "\n"
            str += "---------------\n"
            val penalties = LavalinkLoadBalancer.getPenalties(socket)
            str += "Penalties Total:    " + penalties.total + "\n"
            str += "Player Penalty:          " + penalties.playerPenalty + "\n"
            str += "CPU Penalty:             " + penalties.cpuPenalty + "\n"
            str += "Deficit Frame Penalty:   " + penalties.deficitFramePenalty + "\n"
            str += "Null Frame Penalty:      " + penalties.nullFramePenalty + "\n"
            str += "Raw: " + penalties.toString() + "\n"
            str += "---------------\n\n"

            messages.add(str)
        }

        if (showHosts) {
            messages.forEach {
                context.replyPrivate(TextUtils.asCodeBlock(it))
            }
            context.replyWithName("Sent you a DM with the data. If you did not receive anything, adjust your privacy settings so I can DM you.")
        } else {
            messages.forEach {
                context.reply(TextUtils.asCodeBlock(it))
            }
        }
    }

    override fun help(context: Context): String {
        return ("{0}{1} list (host)"
                + "\n{0}{1} show <name>"
                + "\n{0}{1} add <name> <uri> <pass>"
                + "\n{0}{1} remove <name>"
                + "\n#Show information about connected lavalink nodes, or add or remove lavalink nodes.")
    }
}
