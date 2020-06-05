/*
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
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

package fredboat.audio.player

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import fredboat.config.property.AppConfig
import fredboat.sentinel.Guild
import fredboat.sentinel.TextChannel
import io.prometheus.client.guava.cache.CacheMetricsCollector
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * Created by napster on 24.02.18.
 */
@Component
class MusicTextChannelProvider(appConfig: AppConfig, cacheMetrics: CacheMetricsCollector) {

    companion object {
        private val log = LoggerFactory.getLogger(MusicTextChannelProvider::class.java)
    }

    //guild id <-> channel id
    private val musicTextChannels: Cache<Long, Long> = CacheBuilder.newBuilder()
            .recordStats()
            .concurrencyLevel(appConfig.shardCount)
            .expireAfterAccess(1, TimeUnit.DAYS)
            .build()

    init {
        cacheMetrics.addCache("musicTextChannels", musicTextChannels)
    }

    fun setMusicChannel(textChannel: TextChannel) {
        musicTextChannels.put(textChannel.guild.id, textChannel.id)
    }

    /**
     * @return id of the music textchannel, or 0 if none has been recorded
     */
    private fun getMusicTextChannelId(guildId: Long): Long {
        return musicTextChannels.getIfPresent(guildId) ?: return 0
    }

    /**
     * @return may return null if we never saved  left the guild, the channel was deleted, or there is no channel where we can talk
     * in that guild
     */
    fun getMusicTextChannel(guild: Guild): TextChannel? {
        val textChannel = guild.getTextChannel(getMusicTextChannelId(guild.id))

        if (textChannel != null) {
            return textChannel
        } else {
            log.warn("No currentTC in guild {}! Trying to look up a channel where we can talk...", guild)
            for (tc in guild.textChannels.values) {
                if (tc.canTalk()) {
                    return tc
                }
            }
            return null
        }
    }
}
