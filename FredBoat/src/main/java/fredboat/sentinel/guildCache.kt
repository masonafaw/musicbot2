package fredboat.sentinel

import com.fredboat.sentinel.SentinelExchanges
import com.fredboat.sentinel.entities.GuildSubscribeRequest
import com.google.common.cache.CacheBuilder
import fredboat.audio.lavalink.SentinelLavalink
import fredboat.config.property.AppConfig
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Service
class GuildCache(private val sentinel: Sentinel,
                 private val appConfig: AppConfig,
                 private val lavalink: SentinelLavalink) {

    init {
        @Suppress("LeakingThis")
        INSTANCE = this
    }

    companion object {
        lateinit var INSTANCE: GuildCache
        private val log: Logger = LoggerFactory.getLogger(GuildCache::class.java)
    }

    @Autowired
    /* Cyclic dependency */
    lateinit var rabbitConsumer: RabbitConsumer
    val cache = ConcurrentHashMap<Long, InternalGuild>()

    /** Non-finished requests. Acts as a debounce */
    private val requestCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES) // Just as a precaution
            .build<Long, Mono<Guild?>>()!!

    /**
     * @param id the ID of the guild
     * @param textChannelInvoked optionally the ID of the text channel used,
     *        in case we need to warn the user of long loading times
     * @param skipCache if we should skip the cache and potentially resubscribe
     */
    fun get(
            id: Long,
            textChannelInvoked: Long? = null,
            skipCache: Boolean = false
    ): Mono<Guild?> {
        if (!skipCache) {
            cache[id]?.let { return Mono.just(it) }

            // If a request is already created and not finished, return that instead
            requestCache.getIfPresent(id)?.let { return it }
        }

        val startTime = System.currentTimeMillis()

        val mono = sentinel.genericMonoSendAndReceive<RawGuild?, Guild?>(
                SentinelExchanges.REQUESTS,
                sentinel.tracker.getKey(calculateShardId(id)),
                GuildSubscribeRequest(id, channelInvoked = textChannelInvoked),
                mayBeEmpty = true,
                transform = {
                    transform(startTime, it)
                })
                .timeout(Duration.ofSeconds(30), Mono.error(TimeoutException("Timed out while subscribing to $id")))
                .doFinally {
                    requestCache.invalidate(id)
                }
        requestCache.put(id, mono)
        return mono
    }

    private fun transform(startTime: Long, it: RawGuild?): InternalGuild? {
        if (it == null) return null

        val timeTakenReceive = System.currentTimeMillis() - startTime
        val g = InternalGuild(it)
        cache[g.id] = g
        val timeTakenParse = System.currentTimeMillis() - startTime - timeTakenReceive
        val timeTaken = timeTakenReceive + timeTakenParse

        log.info("Subscribing to {} took {}ms including {}ms parsing time.\nMembers: {}\nChannels: {}\nRoles: {}\n",
                g,
                timeTaken,
                timeTakenParse,
                g.members.size,
                g.textChannels.size + g.voiceChannels.size,
                g.roles.size
        )

        // Asynchronously handle existing VSU from an older FredBoat session, if it exists
        it.voiceServerUpdate?.let { vsu ->
            GlobalScope.launch {
                val channelId = g.selfMember.voiceChannel?.idString

                val link = lavalink.getLink(g)
                if (channelId == null) {
                    log.warn("Received voice server update during guild subscribe, but we are not in a channel." +
                            "This should not happen. Disconnecting...")
                    link.queueAudioDisconnect()
                    return@launch
                }

                link.setChannel(channelId)
                rabbitConsumer.receive(vsu)
                /*
                // This code is an excellent way to test expired voice server updates
                val json = JSONObject(vsu.raw)
                json.put("token", "asd")
                rabbitConsumer.receive(VoiceServerUpdate(vsu.sessionId, json.toString()))
                */
            }
        }

        return g
    }

    fun getIfCached(id: Long): Guild? = cache[id]

    private fun calculateShardId(guildId: Long): Int = ((guildId shr 22) % appConfig.shardCount.toLong()).toInt()

}

/**
 * @param id the ID of the guild
 * @param textChannelInvoked optionally the ID of the text channel used,
 *        in case we need to warn the user of long loading times
 */
suspend fun getGuild(id: Long, textChannelInvoked: Long? = null) = GuildCache.INSTANCE.get(id, textChannelInvoked)
        .awaitFirstOrNull()

/**
 * @param id the ID of the guild
 * @param textChannelInvoked optionally the ID of the text channel used,
 *        in case we need to warn the user of long loading times
 */
fun getGuildMono(id: Long, textChannelInvoked: Long? = null) = GuildCache.INSTANCE.get(id, textChannelInvoked)

/**
 * @param id the ID of the guild
 * @param textChannelInvoked optionally the ID of the text channel used,
 *        in case we need to warn the user of long loading times
 */
fun getGuild(id: Long, textChannelInvoked: Long? = null, callback: (Guild) -> Unit) {
    GuildCache.INSTANCE.get(id, textChannelInvoked).subscribe { callback(it!!) }
}
