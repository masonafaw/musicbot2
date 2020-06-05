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

package fredboat.event

import com.fredboat.sentinel.entities.LifecycleEventEnum
import com.fredboat.sentinel.entities.ShardLifecycleEvent
import fredboat.config.property.EventLoggerConfig
import fredboat.main.ShutdownHandler
import fredboat.sentinel.Guild
import fredboat.util.Emojis
import fredboat.util.TextUtils
import fredboat.util.localMessageBuilder
import fredboat.util.rest.Webhook
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * This overengineered class logs some events via a webhook into Discord.
 *
 *
 * Shards status events and guild join/leave events are collected. In regular intervals messages are created containing
 * the collected information. The created messages are then attempted to be posted via the webhook. A small buffer is
 * used to try to not drop occasionally failed messages, since the reporting of status events is especially important
 * during ongoing connection issues. If the amount of status events is too damn high, summaries are posted.
 */
@Component
class EventLogger(
        eventLoggerConfig: EventLoggerConfig,
        shutdownHandler: ShutdownHandler
) : SentinelEventHandler() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(EventLogger::class.java)
    }

    private val scheduler = Executors.newSingleThreadScheduledExecutor { runnable -> Thread(runnable, "eventlogger") }
    private val eventLogWebhook: Webhook?
    private val guildStatsWebhook: Webhook?

    //saves some messages, so that in case we run into occasional connection issues we dont just drop them due to the webhook timing out
    private val toBeSentEventLog = ConcurrentLinkedQueue<String>()

    private val statusStats = CopyOnWriteArrayList<ShardStatusEvent>()
    private val guildsJoinedEvents = AtomicInteger(0)
    private val guildsLeftEvents = AtomicInteger(0)

    override fun onShardLifecycle(event: ShardLifecycleEvent) {
        if (eventLogWebhook == null) {
            return
        }

        statusStats.add(ShardStatusEvent(event.shard.id, event.change))
    }

    override fun onGuildJoin(guild: Guild) {
        if (guildStatsWebhook == null) {
            return
        }
        guildsJoinedEvents.incrementAndGet()
        log.info("Joined guild {} with {} users", guild, guild.members.size)
    }

    override fun onGuildLeave(guildId: Long, joinTime: Instant) {
        if (guildStatsWebhook == null) {
            return
        }
        guildsLeftEvents.incrementAndGet()
        log.info("Left guild {}", guildId)
    }


    private fun createShutdownHook(shutdownHandler: ShutdownHandler): Runnable {
        return Runnable {
            scheduler.shutdownNow()
            val message: String
            val shutdownCode = shutdownHandler.shutdownCode
            message = if (shutdownCode != ShutdownHandler.UNKNOWN_SHUTDOWN_CODE) {
                "${Emojis.DOOR}Exiting with code $shutdownCode."
            } else {
                "${Emojis.DOOR}Exiting with unknown code."
            }
            log.info(message)
            var elw: Mono<Unit>? = null
            var gsw: Mono<Unit>? = null
            if (eventLogWebhook != null) elw = eventLogWebhook.send(message)
            if (guildStatsWebhook != null) gsw = guildStatsWebhook.send(message)
            elw?.block(Duration.ofSeconds(30))
            gsw?.block(Duration.ofSeconds(30))
        }
    }

    init {
        Runtime.getRuntime().addShutdownHook(Thread(createShutdownHook(shutdownHandler), EventLogger::class.java.simpleName + " shutdownhook"))

        val eventLoggerWebhookUrl = eventLoggerConfig.eventLogWebhook
        var eventLoggerWebhook: Webhook? = null
        if (!eventLoggerWebhookUrl.isEmpty()) {
            try {
                eventLoggerWebhook = Webhook(eventLoggerWebhookUrl)
            } catch (e: IllegalArgumentException) {
                log.error("Eventlogger webhook url could not be parsed: {}", eventLoggerWebhookUrl, e)
            }

        }

        //test the provided webhooks before assigning them, otherwise they will spam our logs with exceptions
        var workingWebhook: Webhook? = null
        if (eventLoggerWebhook != null) {
            try {
                eventLoggerWebhook.send(Emojis.PENCIL + "Event logger started.").block(Duration.ofSeconds(30))
                workingWebhook = eventLoggerWebhook //webhook test was successful; FIXME occasionally this might fail during the start due to connection issues, while the provided values are actually valid
            } catch (e: Exception) {
                log.error("Failed to create event log webhook. Event logs will not be available. Doublecheck your configuration values.")
            }
        }
        this.eventLogWebhook = workingWebhook

        if (eventLogWebhook != null) {
            scheduler.scheduleAtFixedRate({
                try {
                    sendEventLogs()
                } catch (e: Exception) {
                    log.error("Failed to send shard status summary to event log webhook", e)
                }
            }, 0, Math.max(eventLoggerConfig.eventLogInterval, 1).toLong(), TimeUnit.MINUTES)
        }


        val guildStatsWebhookUrl = eventLoggerConfig.guildStatsWebhook
        var guildStatsWebhook: Webhook? = null
        if (!guildStatsWebhookUrl.isEmpty()) {
            try {
                guildStatsWebhook = Webhook(guildStatsWebhookUrl)
            } catch (e: IllegalArgumentException) {
                log.error("Guildstats webhook url could not be parsed: {}", guildStatsWebhookUrl, e)
            }

        }

        workingWebhook = null
        if (guildStatsWebhook != null) {
                try {
                    guildStatsWebhook.send(Emojis.PENCIL + "Guild stats logger started.").block(Duration.ofSeconds(30))
                    workingWebhook = guildStatsWebhook //webhook test was successful; FIXME occasionally this might fail during the start due to connection issues, while the provided values are actually valid
                } catch (e: Exception) {
                    log.error("Failed to create guild stats webhook. Guild stats will not be available. Doublecheck your configuration values.")
                }
        }
        this.guildStatsWebhook = workingWebhook
    }

    private fun sendEventLogs() {
        if (eventLogWebhook == null) {
            return
        }
        val events = ArrayList(statusStats)
        statusStats.removeAll(events)

        if (events.isEmpty()) {
            return //nothing to report
        }

        //split into messages of acceptable size (2k chars max)
        var msg = StringBuilder()
        for (event in events) {
            val eventStr = event.toString()
            if (msg.length + eventStr.length > 1900) {
                toBeSentEventLog.add(localMessageBuilder().codeBlock(msg.toString(), "diff").build())
                msg = StringBuilder()
            }
            msg.append("\n").append(eventStr)
        }
        if (msg.isNotEmpty()) {//any leftovers?
            toBeSentEventLog.add(localMessageBuilder().codeBlock(msg.toString(), "diff").build())
        }
        drainMessageQueue(toBeSentEventLog, eventLogWebhook)
    }

    private fun drainMessageQueue(queue: Queue<String>, webhook: Webhook) {
        try {
            while (!queue.isEmpty()) {
                val message = queue.peek()
                webhook.send(message).block(Duration.ofSeconds(30))
                queue.poll()
            }
        } catch (e: Exception) {
            log.warn("Webhook failed to send a message. Will try again next time.", e)
        }

    }

    private class ShardStatusEvent(
            internal val shardId: Int,
            internal val event: LifecycleEventEnum,
            internal val additionalInfo: String = ""
    ) {
        internal val timestamp: Long = System.currentTimeMillis()

        /*internal enum class StatusEvent(var str: String, var diff: String) {
            READY("readied", "+"),
            RESUME("resumed", "+"),
            RECONNECT("reconnected", "+"),
            DISCONNECT("disconnected", "-"),
            SHUTDOWN("shut down", "-")
        }*/

        override fun toString(): String {
            val verb = when (event) {
                LifecycleEventEnum.READIED -> "readied"
                LifecycleEventEnum.DISCONNECTED -> "disconnected"
                LifecycleEventEnum.RESUMED -> "resumed"
                LifecycleEventEnum.RECONNECTED -> "reconnected"
                LifecycleEventEnum.SHUTDOWN -> "shutdown"
            }

            val diff = when (event) {
                LifecycleEventEnum.READIED -> "+"
                LifecycleEventEnum.DISCONNECTED -> "+"
                LifecycleEventEnum.RESUMED -> "+"
                LifecycleEventEnum.RECONNECTED -> "-"
                LifecycleEventEnum.SHUTDOWN -> "-"
            }

            return String.format("%s [%s] Shard %s %s %s",
                    diff, TextUtils.asTimeInCentralEurope(timestamp),
                    TextUtils.forceNDigits(shardId, 3), verb, additionalInfo)
        }

        override fun hashCode(): Int {
            return Objects.hash(shardId, event, timestamp)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ShardStatusEvent

            if (shardId != other.shardId) return false
            if (event != other.event) return false
            if (timestamp != other.timestamp) return false

            return true
        }
    }
}
