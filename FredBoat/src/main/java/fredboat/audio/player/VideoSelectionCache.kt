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

import com.google.common.cache.CacheBuilder
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import fredboat.commandmeta.abs.CommandContext
import fredboat.sentinel.Member
import io.prometheus.client.guava.cache.CacheMetricsCollector
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * Cache and provide video selections (aka search results shown to users, for them to select from)
 */
@Component
class VideoSelectionCache(
        cacheMetrics: CacheMetricsCollector
) {

    //the key looks like this: guildId:userId
    val videoSelections = CacheBuilder.newBuilder()
            .recordStats()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .build<String, VideoSelection>()!!

    init {
        cacheMetrics.addCache("videoSelections", videoSelections)
    }

    operator fun get(member: Member): VideoSelection? {
        return get(member.guild.id, member.id)
    }

    fun remove(member: Member): VideoSelection? {
        return remove(member.guild.id, member.id)
    }

    fun put(message: Long, context: CommandContext, choices: List<AudioTrack>, isPriority: Boolean) {
        videoSelections.put(asKey(context.member), VideoSelection(message, context, choices, isPriority))
    }

    private operator fun get(guildId: Long, userId: Long): VideoSelection? {
        return videoSelections.getIfPresent(asKey(guildId, userId))
    }

    private fun remove(guildId: Long, userId: Long): VideoSelection? {
        val result = get(guildId, userId)
        videoSelections.invalidate(asKey(guildId, userId))
        return result
    }


    private fun asKey(member: Member): String {
        return asKey(member.guild.id, member.id)
    }

    private fun asKey(guildId: Long, userId: Long): String {
        return guildId.toString() + ":" + userId
    }

    data class VideoSelection internal constructor(
            val message: Long,
            val context: CommandContext,
            val choices: List<AudioTrack>,
            val isPriority: Boolean
    ) {
        fun deleteMessage() {
            context.textChannel.deleteMessage(message).subscribe()
        }
    }
}
