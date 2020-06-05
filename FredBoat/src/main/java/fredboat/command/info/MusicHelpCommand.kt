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

import fredboat.command.music.control.*
import fredboat.command.music.info.*
import fredboat.command.music.seeking.ForwardCommand
import fredboat.command.music.seeking.RestartCommand
import fredboat.command.music.seeking.RewindCommand
import fredboat.command.music.seeking.SeekCommand
import fredboat.commandmeta.CommandInitializer
import fredboat.commandmeta.CommandRegistry
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IInfoCommand
import fredboat.definitions.Module
import fredboat.definitions.PermissionLevel
import fredboat.messaging.internal.Context
import fredboat.perms.Permission
import fredboat.perms.PermsUtil
import fredboat.sentinel.getGuild
import fredboat.shared.constant.BotConstants
import fredboat.util.Emojis
import fredboat.util.TextUtils
import java.util.*
import kotlin.streams.toList

class MusicHelpCommand(name: String, vararg aliases: String) : Command(name, *aliases), IInfoCommand {

    companion object {
        private const val HERE = "here"
        private const val UPDATE = "update"
    }

    override suspend fun invoke(context: CommandContext) {

        if (context.args.size > 2 && context.args[0].toLowerCase().contains(UPDATE)) {
            updateMessage(context)
            return
        }

        var postInDm = true
        if (context.rawArgs.toLowerCase().contains(HERE)) {
            postInDm = false
        }

        val messages = getMessages(context)
        var lastOne = ""
        if (postInDm) {
            lastOne = messages.removeAt(messages.size - 1)
        }

        for (message in messages) {
            if (postInDm) {
                context.replyPrivate(message)
            } else {
                context.reply(message)
            }
        }

        if (postInDm) {
            context.replyPrivateMono(lastOne)
                    .doOnError {
                        if (context.hasPermissions(Permission.MESSAGE_WRITE)) {
                            context.replyWithName(Emojis.EXCLAMATION + context.i18n("helpDmFailed"))
                        } }
                    .subscribe{ context.replyWithName(context.i18n("helpSent")) }
        }
    }

    override fun help(context: Context): String {
        return "{0}{1} OR {0}{1} " + HERE + "\n#" + context.i18n("helpMusicHelpCommand")
    }

    private suspend fun updateMessage(context: CommandContext) {
        //this method is intentionally undocumented cause Napster cba to i18n it as this is intended for FBH mainly
        if (!PermsUtil.checkPermsWithFeedback(PermissionLevel.ADMIN, context)) {
            return
        }
        val channelId: Long
        val messageId: Long
        try {
            channelId = java.lang.Long.parseUnsignedLong(context.args[1])
            messageId = java.lang.Long.parseUnsignedLong(context.args[2])
        } catch (e: NumberFormatException) {
            context.reply("Could not parse the provided channel and/or message ids.")
            return
        }

        val fbhMusicCommandsChannel = getGuild(BotConstants.FREDBOAT_HANGOUT_ID)?.getTextChannel(channelId)
        if (fbhMusicCommandsChannel == null) {
            context.reply("Could not find the requested channel with id $channelId")
            return
        }
        val messages = getMessages(context)
        if (messages.size > 1) {
            context.reply(Emojis.EXCLAMATION + "The music help is longer than one message, only the first one will be" +
                    " edited in.")
        }

        fbhMusicCommandsChannel.editMessage(messageId, getMessages(context)[0])
                .doOnError { context.reply("Could not find the message with id $messageId or it is not a message that" +
                        " I'm allowed to edit.") }
                .subscribe()
    }

    //returns the music commands ready to be posted to a channel
    // may return a list with more than one message if we hit the message size limit which might happen due to long
    // custom prefixes, or translations that take more letters than the stock english one
    // stock english commands with stock prefix should always aim to stay in one message
    // if this won't be possible in the future as more commands get added or regular commands get more complicated and
    // require longer helps, its is possible to use an embed, which has a much higher character limit (6k vs 2k currently)
    private fun getMessages(context: Context): MutableList<String> {
        val messages = ArrayList<String>()
        val musicComms = getSortedMusicComms(context)

        var out = StringBuilder("< " + context.i18n("helpMusicCommandsHeader") + " >\n")
        for (s in musicComms) {
            if (out.length + s.length >= 1990) {
                val block = TextUtils.asCodeBlock(out.toString(), "md")
                messages.add(block)
                out = StringBuilder()
            }
            out.append(s).append("\n")
        }
        val block = TextUtils.asCodeBlock(out.toString(), "md")
        messages.add(block)
        return messages
    }

    private fun getSortedMusicComms(context: Context): List<String> {
        var musicCommands: List<Command> = CommandRegistry.getCommandModule(Module.MUSIC).deduplicatedCommands

        //dont explicitly show the youtube and soundcloud commands in this list, since they are just castrated versions
        // of the play command, which is "good enough" for this list
        musicCommands = musicCommands.stream()
                .filter { command -> !(command is PlayCommand && (command.name == CommandInitializer.YOUTUBE_COMM_NAME || command.name == CommandInitializer.SOUNDCLOUD_COMM_NAME)) }
                .filter { command -> command !is DestroyCommand }
                .toList().toMutableList()

        musicCommands.sortWith(MusicCommandsComparator())

        val musicComms = ArrayList<String>()
        for (command in musicCommands) {
            val formattedHelp = HelpCommand.getFormattedCommandHelp(context, command, command.name)
            musicComms.add(formattedHelp)
        }

        return musicComms
    }

    /**
     * Sort the commands in a sensible way to display them to the user
     */
    internal class MusicCommandsComparator : Comparator<Command> {

        override fun compare(o1: Command, o2: Command): Int {
            return getCommandRank(o1) - getCommandRank(o2)
        }

        companion object {

            private val commandOrdering = Arrays.asList<Class<out Command>>(
                    PlayCommand::class.java,
                    ListCommand::class.java,
                    NowplayingCommand::class.java,
                    SkipCommand::class.java,
                    VoteSkipCommand::class.java,
                    StopCommand::class.java,
                    PauseCommand::class.java,
                    UnpauseCommand::class.java,
                    JoinCommand::class.java,
                    LeaveCommand::class.java,
                    RepeatCommand::class.java,
                    ShuffleCommand::class.java,
                    ReshuffleCommand::class.java,
                    ForwardCommand::class.java,
                    RewindCommand::class.java,
                    SeekCommand::class.java,
                    RestartCommand::class.java,
                    HistoryCommand::class.java,
                    ExportCommand::class.java,
                    PlaySplitCommand::class.java,
                    SelectCommand::class.java,
                    GensokyoRadioCommand::class.java,
                    VolumeCommand::class.java,
                    DestroyCommand::class.java
            )

            /**
             * a container of smelly code
             * http://stackoverflow.com/a/2790215
             */
            private fun getCommandRank(c: Command): Int {
                val rank = commandOrdering.indexOf(c.javaClass)

                return if (rank == -1) {
                    commandOrdering.size //put at the end
                } else {
                    rank
                }
            }
        }
    }
}
