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

import com.fredboat.sentinel.entities.SendMessageResponse
import fredboat.command.info.HelpCommand
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.definitions.PermissionLevel
import fredboat.main.Launcher
import fredboat.messaging.internal.Context
import fredboat.util.TextUtils
import fredboat.util.extension.edit
import org.slf4j.LoggerFactory
import java.text.MessageFormat
import java.util.concurrent.Phaser
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * @author frederik
 */
class AnnounceCommand(name: String, vararg aliases: String) : Command(name, *aliases), ICommandRestricted {

    companion object {
        private val log = LoggerFactory.getLogger(AnnounceCommand::class.java)
        private const val HEAD = "__**[BROADCASTED MESSAGE]**__\n"
    }

    override val minimumPerms: PermissionLevel
        get() = PermissionLevel.BOT_ADMIN

    override suspend fun invoke(context: CommandContext) {
        val players = Launcher.botController.playerRegistry.playingPlayers

        if (players.isEmpty()) {
            context.reply("No currently playing players.")
            return
        }
        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context)
            return
        }

        val msg = HEAD + context.rawArgs

        var mono = context.replyMono("[0/${players.size}]")
        mono = mono.doOnSuccess { status ->
            Thread {
                val phaser = Phaser(players.size)

                for (player in players) {
                    val activeTextChannel = player.activeTextChannel
                    if (activeTextChannel != null) {
                        activeTextChannel.send(msg)
                                .doOnError { phaser.arriveAndDeregister() }
                                .subscribe { phaser.arrive() }
                    } else {
                        phaser.arriveAndDeregister()
                    }
                }

                Thread {
                    try {
                        do {
                            try {
                                phaser.awaitAdvanceInterruptibly(0, 5, TimeUnit.SECONDS)
                                // Now all the parties have arrived, we can break out of the loop
                                break
                            } catch (ex: TimeoutException) {
                                // This is fine, this means that the required parties haven't arrived
                            }

                            printProgress(context,
                                    status,
                                    phaser.arrivedParties,
                                    players.size,
                                    players.size - phaser.registeredParties)
                        } while (true)
                        printDone(context,
                                status,
                                phaser.registeredParties, //phaser wraps back to 0 on phase increment
                                players.size - phaser.registeredParties)
                    } catch (ex: InterruptedException) {
                        Thread.currentThread().interrupt() // restore interrupt flag
                        log.error("interrupted", ex)
                        throw RuntimeException(ex)
                    }
                }.start()
            }.start()
        }
        mono.doOnError { throwable ->
            TextUtils.handleException("Announcement failed!", throwable, context)
            throw RuntimeException(throwable)
        }.subscribe()
    }

    private fun printProgress(context: CommandContext, message: SendMessageResponse, done: Int, total: Int, error: Int) {
        message.edit(context.textChannel, MessageFormat.format(
                "[{0}/{1}]{2,choice,0#|0< {2} failed}",
                done, total, error)
        ).subscribe()
    }

    private fun printDone(context: CommandContext, message: SendMessageResponse, completed: Int, failed: Int) {
        message.edit(context.textChannel, "Done with $completed completed and $failed failed").subscribe()
    }

    override fun help(context: Context) =
            "{0}{1} <announcement>\n#Broadcast an announcement to active textchannels of playing GuildPlayers."
}
