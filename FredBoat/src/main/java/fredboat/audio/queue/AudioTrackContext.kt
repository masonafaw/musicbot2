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

package fredboat.audio.queue

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import fredboat.audio.player.GuildPlayer
import fredboat.feature.I18n
import fredboat.main.Launcher
import fredboat.sentinel.Member
import fredboat.sentinel.TextChannel
import java.util.concurrent.ThreadLocalRandom

open class AudioTrackContext(val track: AudioTrack, val member: Member, priority: Boolean = false) : Comparable<AudioTrackContext> {
    val added: Long = System.currentTimeMillis()
    var rand: Int = 0
    var isPriority: Boolean = priority
    val trackId: Long //used to identify this track even when the track gets cloned and the rand reranded

    val userId: Long
        get() = member.id

    val guildId: Long
        get() = member.guild.id

    open val effectiveDuration: Long
        get() = track.duration

    open val effectiveTitle: String
        get() = track.info.title

    open val startPosition: Long
        get() = 0

    //return the currently active text channel of the associated guildplayer
    val textChannel: TextChannel?
        get() {
            val guildPlayer = Launcher.botController.playerRegistry.getExisting(guildId)
            return guildPlayer?.activeTextChannel
        }

    init {
        this.rand = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE)
        this.trackId = ThreadLocalRandom.current().nextLong(java.lang.Long.MAX_VALUE)
    }//It's ok to set a non-existing channelId, since inside the AudioTrackContext, the channel needs to be looked up
    // every time. See the getTextChannel() below for doing that.

    fun randomize(): Int {
        rand = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE)
        return rand
    }

    open fun makeClone(): AudioTrackContext {
        return AudioTrackContext(track.makeClone(), member, isPriority)
    }

    //NOTE: convenience method that returns the position of the track currently playing in the guild where this track was added
    open fun getEffectivePosition(guildPlayer: GuildPlayer): Long {
        return guildPlayer.position
    }

    override fun compareTo(other: AudioTrackContext): Int {
        return Integer.compare(rand, other.rand)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioTrackContext) return false

        if (track != other.track) return false
        if (member != other.member) return false
        if (trackId != other.trackId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = track.hashCode()
        result = 31 * result + member.hashCode()
        result = 31 * result + trackId.hashCode()
        return result
    }

    fun i18n(key: String) = I18n.get(guildId).getString(key)!!
    fun i18nFormat(key: String, vararg values: Any): String {
        var str = i18n(key)
        values.forEachIndexed { i, v -> str = str.replace("{$i}", v.toString()) }
        return str
    }


}
