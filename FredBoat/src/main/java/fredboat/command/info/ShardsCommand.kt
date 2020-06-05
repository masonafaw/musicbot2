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

import com.fredboat.sentinel.entities.ShardStatus
import com.fredboat.sentinel.entities.shardString
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.commandmeta.abs.IInfoCommand
import fredboat.definitions.PermissionLevel
import fredboat.main.Launcher
import fredboat.messaging.internal.Context
import fredboat.sentinel.Sentinel
import fredboat.sentinel.Sentinel.NamedSentinelInfoResponse
import fredboat.util.extension.asCodeBlock
import kotlinx.coroutines.reactive.awaitLast
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class ShardsCommand(name: String, vararg aliases: String) : Command(name, *aliases), IInfoCommand, ICommandRestricted {

    override val minimumPerms = PermissionLevel.ADMIN

    override suspend fun invoke(context: CommandContext) {
        context.sendTyping()
        for (message in getShardStatus(context.sentinel, context.msg.content)) {
            context.reply(message)
        }
    }

    companion object {
        private const val SHARDS_PER_MESSAGE = 30

        suspend fun getShardStatus(sentinel: Sentinel, input: String): List<String> {
            val lines = ArrayList<String>()

            //do a full report? or just a summary
            val raw = input.toLowerCase()
            val full = raw.contains("full") || raw.contains("all")

            val shardCounter = AtomicInteger(0)
            val borkenShards = AtomicInteger(0)
            val healthyGuilds = AtomicLong(0)
            val healthyUsers = AtomicLong(0)
            val sentinelRoutingKeys = sentinel.tracker.sentinels.map { it.key }

            // Collect all the info, even if the upstream may produce an exception
            // We will still throw an exception if we got no info (that should never happen)
            val sentinelList = mutableListOf<NamedSentinelInfoResponse>()
            try {
                sentinel.getAllSentinelInfo(includeShards = true)
                        .doOnNext { sentinelList.add(it) }
                        .awaitLast()
            } catch (e: Exception) {
                if (sentinelList.size == 0) throw e
            }

            val shards = sentinelList.flatMap { it.response.shards!! }
                    .sortedBy { it.shard.id }

            shards.forEach { shard ->
                shardCounter.incrementAndGet()
                if (shard.shard.status == ShardStatus.CONNECTED && !full) {
                    healthyGuilds.addAndGet(shard.guilds.toLong())
                    healthyUsers.addAndGet(shard.users.toLong())
                } else {

                    lines.add((if (shard.shard.status == ShardStatus.CONNECTED) "+" else "-")
                            + " "
                            + shard.shard.shardString
                            + " "
                            + shard.shard.status
                            + " -- Guilds: "
                            + String.format("%04d", shard.guilds)
                            + " -- Users: "
                            + shard.users
                            + "\n")
                    borkenShards.incrementAndGet()
                }
            }

            val messages = ArrayList<String>()

            var stringBuilder = StringBuilder()
            var lineCounter = 0
            for (line in lines) {
                stringBuilder.append(line)
                lineCounter++
                if (lineCounter % SHARDS_PER_MESSAGE == 0 || lineCounter == lines.size) {
                    messages.add(stringBuilder.toString().asCodeBlock("diff"))
                    stringBuilder = StringBuilder()
                }
            }

            //healthy shards summary, contains sensible data only if we aren't doing a full report
            if (!full) {
                val content = String.format("+ %s of %s shards are %s -- Guilds: %s -- Users: %s", shardCounter.get() - borkenShards.get(),
                        Launcher.botController.appConfig.shardCount, ShardStatus.CONNECTED, healthyGuilds, healthyUsers)
                messages.add(0, content.asCodeBlock("diff"))
            }

            // Report any unresponsive Sentinels
            val responsive = sentinelList.map { it.routingKey }
            val unresponsive = sentinelRoutingKeys.toMutableList()
            unresponsive.removeAll(responsive)

            if (unresponsive.isNotEmpty()) {
                val builder = StringBuilder("The following sentinels did not respond:")
                unresponsive.forEach { builder.append("- $it") }
                messages.add(builder.toString().asCodeBlock("diff"))
            }

            return messages
        }

    }

    override fun help(context: Context): String {
        return "{0}{1} [full]\n#Show information about the shards of the bot as a summary or in a detailed report."
    }
}
