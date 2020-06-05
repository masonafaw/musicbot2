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

package fredboat.command.info

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary
import fredboat.agent.FredBoatAgent
import fredboat.commandmeta.CommandManager
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IInfoCommand
import fredboat.feature.I18n
import fredboat.main.Launcher
import fredboat.main.getBotController
import fredboat.messaging.internal.Context
import fredboat.sentinel.RawUser
import fredboat.util.AppInfo
import fredboat.util.DiscordUtil
import fredboat.util.TextUtils
import kotlinx.coroutines.reactive.awaitSingle
import java.text.MessageFormat
import java.util.*

class StatsCommand(
        name: String,
        selfUser: RawUser,
        vararg aliases: String
) : Command(name, *aliases), IInfoCommand {

    init {
        botId = selfUser.id
    }

    override suspend fun invoke(context: CommandContext) {
        context.reply(getStats(context))
    }

    override fun help(context: Context): String {
        return "{0}{1}\n#Show some statistics about this bot."
    }

    companion object {
        private var botId = 0L

        suspend fun getStats(context: Context?): String {
            context?.sendTyping()

            val totalSecs = (System.currentTimeMillis() - Launcher.START_TIME) / 1000
            val days = (totalSecs / (60 * 60 * 24)).toInt()
            val hours = (totalSecs / (60 * 60) % 24).toInt()
            val mins = (totalSecs / 60 % 60).toInt()
            val secs = (totalSecs % 60).toInt()

            // Sorry for the ugly i18n handling but thats the price
            // we pay for this to be accessible for bot admins through DMs
            val i18n: ResourceBundle = context?.getI18n() ?: I18n.DEFAULT.props

            val commandsExecuted = CommandManager.totalCommandsExecuted.get().toDouble()
            var str = MessageFormat.format(i18n.getString("statsParagraph"),
                    days, hours, mins, secs, commandsExecuted - 1) + "\n"
            str = MessageFormat.format(i18n.getString("statsRate"), str,
                    (commandsExecuted - 1).toFloat() / (totalSecs.toFloat() / (60 * 60).toFloat()))

            str += "\n\n"
            var content = ""
            val botMetrics = Launcher.botController.botMetrics

            content += "Reserved memory:                " + Runtime.getRuntime().totalMemory() / 1000000 + "MB\n"
            content += "-> Of which is used:            " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000 + "MB\n"
            content += "-> Of which is free:            " + Runtime.getRuntime().freeMemory() / 1000000 + "MB\n"
            content += "Max reservable:                 " + Runtime.getRuntime().maxMemory() / 1000000 + "MB\n"

            content += "\n----------\n\n"

            content += "Sharding:                       " + context?.guild?.shardString + "\n"
            content += "Music players playing:          " + Launcher.botController.playerRegistry.playingCount() + "\n"

            val counts = getBotController().sentinelCountingService.getAllCountsCached()
            //val not = "not counted yet"
            content += "Known servers:                  " + counts.first.guilds + "\n"
            content += "Users in servers:               " + counts.second + "\n"
            content += "Text channels:                  " + counts.first.textChannels + "\n"
            content += "Voice channels:                 " + counts.first.voiceChannels + "\n"
            content += "Categories:                     " + counts.first.categories + "\n"
            content += "Roles:                          " + counts.first.roles + "\n"

            content += "\n----------\n\n"

            content += "Distribution:                   " + Launcher.botController.appConfig.distribution + "\n"
            content += "FredBoat version:               " + AppInfo.getAppInfo().versionBuild + "\n"
            content += "Lavaplayer version:             " + PlayerLibrary.VERSION + "\n"

            content += "\n----------\n\n"
            if (DiscordUtil.isOfficialBot(botId)) {
                val dockerStats = botMetrics.dockerStats
                content += "Docker pulls:\n"
                content += "    FredBoat image:             " + dockerStats.dockerPullsBot + "\n"
                content += "    Database image:             " + dockerStats.dockerPullsDb + "\n"
                content += "\n----------\n\n"
            }

            content += "Last agent run times:\n"
            for ((key, value) in FredBoatAgent.getLastRunTimes()) {
                // [classname] [padded to length 32 with spaces] [formatted time]
                content += String.format("%1$-32s%2\$s\n", key.simpleName,
                        TextUtils.asTimeInCentralEurope(value))
            }

            str += TextUtils.asCodeBlock(content)

            return str
        }
    }
}
