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

package fredboat.audio.player

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import fredboat.audio.lavalink.SentinelLavalink
import fredboat.db.api.GuildConfigService
import fredboat.sentinel.Guild
import fredboat.util.ratelimit.Ratelimiter
import fredboat.util.rest.YoutubeAPI
import lavalink.client.io.Link.State
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiConsumer
import kotlin.streams.toList

@Component
class PlayerRegistry(private val musicTextChannelProvider: MusicTextChannelProvider,
                     private val guildConfigService: GuildConfigService, private val lavalink: SentinelLavalink,
                     @param:Qualifier("loadAudioPlayerManager") val audioPlayerManager: AudioPlayerManager,
                     private val ratelimiter: Ratelimiter, private val youtubeAPI: YoutubeAPI) {

    companion object {
        const val DEFAULT_VOLUME = 1f
        private val log: Logger = LoggerFactory.getLogger(PlayerRegistry::class.java)
    }

    private val registry = ConcurrentHashMap<Long, GuildPlayer>()

    private val iteratorLock = Any() //iterators, which are also used by stream(), need to be synced, despite it being a concurrent map

    /**
     * @return a copied list of the the playing players of the registry. This may be an expensive operation depending on
     * the size, don't use this in code that is called often. Instead, have a look at other methods like
     * [PlayerRegistry.playingCount] which might fulfill your needs without creating intermediary giant list objects.
     */
    val playingPlayers: List<GuildPlayer>
        get() = synchronized(iteratorLock) {
            return registry.values.stream()
                    .filter { it.isPlaying }
                    .toList()
        }

    fun getOrCreate(guild: Guild): GuildPlayer {
        return registry.computeIfAbsent(
                guild.id) {
            val p = GuildPlayer(lavalink, guild, musicTextChannelProvider, audioPlayerManager, guildConfigService,
                    ratelimiter, youtubeAPI)
            p.volume = DEFAULT_VOLUME
            p
        }
    }

    fun getExisting(guild: Guild): GuildPlayer? {
        return getExisting(guild.id)
    }

    fun getExisting(guildId: Long): GuildPlayer? {
        return registry[guildId]
    }

    fun forEach(consumer: BiConsumer<Long, GuildPlayer>) {
        registry.forEach(consumer)
    }

    fun destroyPlayer(g: Guild) {
        destroyPlayer(g.id)
    }

    fun destroyPlayer(guildId: Long) {
        val player = getExisting(guildId)
        if (player != null) {
            if (player.player.link.state == State.DESTROYED) {
                log.warn("Attempt to destroy already destroyed player." +
                        " This should not happen. Removing without re-destroying...")
            } else {
                player.destroy()
            }
            registry.remove(guildId)
        }
    }

    fun totalCount(): Long {
        return registry.size.toLong()
    }

    fun playingCount(): Long {
        synchronized(iteratorLock) {
            return registry.values.stream()
                    .filter { it.isPlaying }
                    .count()
        }
    }
}
