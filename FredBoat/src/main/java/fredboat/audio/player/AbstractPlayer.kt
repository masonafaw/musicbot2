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

import com.google.common.collect.Lists
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.TrackMarker
import fredboat.audio.lavalink.SentinelLavalink
import fredboat.audio.queue.AudioTrackContext
import fredboat.audio.queue.ITrackProvider
import fredboat.audio.queue.SplitAudioTrackContext
import fredboat.audio.queue.TrackEndMarkerHandler
import fredboat.commandmeta.MessagingException
import fredboat.sentinel.Guild
import fredboat.util.TextUtils
import lavalink.client.player.LavalinkPlayer
import lavalink.client.player.event.AudioEventAdapterWrapped
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Consumer

abstract class AbstractPlayer internal constructor(
        lavalink: SentinelLavalink,
        internal val audioTrackProvider: ITrackProvider,
        guild: Guild
) : AudioEventAdapterWrapped() {

    val player: LavalinkPlayer = lavalink.getLink(guild.id.toString()).player
    protected var context: AudioTrackContext? = null

    internal var onPlayHook: Consumer<AudioTrackContext>? = null
    internal var onErrorHook: Consumer<Throwable>? = null
    @Volatile
    private var lastLoadedTrack: AudioTrackContext? = null
    private val historyQueue = ConcurrentLinkedQueue<AudioTrackContext>()

    companion object {
        private val log = LoggerFactory.getLogger(AbstractPlayer::class.java)
        private const val MAX_HISTORY_SIZE = 20
    }

    val isQueueEmpty: Boolean
        get() {
            log.trace("isQueueEmpty()")

            return player.playingTrack == null && audioTrackProvider.isEmpty
        }

    val trackCountInHistory: Int
        get() = historyQueue.size

    val isHistoryQueueEmpty: Boolean
        get() = historyQueue.isEmpty()

    val playingTrack: AudioTrackContext?
        get() {
            log.trace("getPlayingTrack()")

            return if (player.playingTrack == null && context == null) {
                audioTrackProvider.peek()
            } else context
        }

    //the unshuffled playlist
    //Includes currently playing track, which comes first
    val remainingTracks: List<AudioTrackContext>
        get() {
            log.trace("getRemainingTracks()")
            val list = ArrayList<AudioTrackContext>()
            val atc = playingTrack
            if (atc != null) {
                list.add(atc)
            }

            list.addAll(audioTrackProvider.asList)
            return list
        }

    var volume: Float
        get() = player.volume.toFloat() / 100
        set(vol) {
            player.volume = (vol * 100).toInt()
        }

    val isPlaying: Boolean
        get() = player.playingTrack != null && !player.isPaused

    val isPaused: Boolean
        get() = player.isPaused

    val position: Long
        get() = player.trackPosition

    init {
        player.addListener(this)
    }

    fun play() {
        log.trace("play()")

        if (player.isPaused) {
            player.isPaused = false
        }
        if (player.playingTrack == null) {
            loadAndPlay()
        }

    }

    fun setPause(pause: Boolean) {
        log.trace("setPause({})", pause)

        if (pause) {
            player.isPaused = true
        } else {
            player.isPaused = false
            play()
        }
    }

    /**
     * Pause the player
     */
    fun pause() {
        log.trace("pause()")

        player.isPaused = true
    }

    /**
     * Clear the tracklist and stop the current track
     */
    fun stop() {
        log.trace("stop()")

        audioTrackProvider.clear()
        stopTrack()
    }

    /**
     * Skip the current track
     */
    fun skip() {
        log.trace("skip()")

        audioTrackProvider.skipped()
        stopTrack()
    }

    /**
     * Stop the current track.
     */
    fun stopTrack() {
        log.trace("stopTrack()")

        context = null
        player.stopTrack()
    }

    fun getTracksInHistory(start: Int, end: Int): List<AudioTrackContext> {
        val start2 = Math.max(start, 0)
        val end2 = Math.max(end, start)
        val historyList = ArrayList(historyQueue)

        return if (historyList.size >= end2) {
            Lists.reverse(ArrayList(historyQueue)).subList(start2, end2)
        } else {
            ArrayList()
        }
    }

    override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, endReason: AudioTrackEndReason?) {
        log.debug("onTrackEnd({} {} {}) called", track!!.info.title, endReason!!.name, endReason.mayStartNext)

        if (endReason == AudioTrackEndReason.FINISHED || endReason == AudioTrackEndReason.STOPPED) {
            updateHistoryQueue()
            loadAndPlay()
        } else if (endReason == AudioTrackEndReason.CLEANUP) {
            log.info("Track " + track.identifier + " was cleaned up")
        } else if (endReason == AudioTrackEndReason.LOAD_FAILED) {
            if (onErrorHook != null)
                onErrorHook!!.accept(MessagingException("Track `" + TextUtils.escapeAndDefuse(track.info.title) + "` failed to load. Skipping..."))
            audioTrackProvider.skipped()
            loadAndPlay()
        } else {
            log.warn("Track " + track.identifier + " ended with unexpected reason: " + endReason)
        }
    }

    //request the next track from the track provider and start playing it
    private fun loadAndPlay() {
        log.trace("loadAndPlay()")

        val atc = audioTrackProvider.provideAudioTrack()
        lastLoadedTrack = atc
        atc?.let { playTrack(it) }
    }

    private fun updateHistoryQueue() {
        val lastTrack = lastLoadedTrack
        if (lastTrack == null) {
            log.debug("No lastLoadedTrack in $this after track end")
            return
        }
        if (historyQueue.size == MAX_HISTORY_SIZE) {
            historyQueue.poll()
        }
        historyQueue.add(lastTrack)
    }

    /**
     * Plays the provided track.
     *
     *
     * Silently playing a track will not trigger the onPlayHook (which announces the track usually)
     */
    private fun playTrack(trackContext: AudioTrackContext, silent: Boolean = false) {
        log.trace("playTrack({})", trackContext.effectiveTitle)

        context = trackContext
        player.playTrack(trackContext.track)
        trackContext.track.position = trackContext.startPosition

        if (trackContext is SplitAudioTrackContext) {
            //Ensure we don't step over our bounds
            log.info("Start: ${trackContext.startPosition} End: ${trackContext.startPosition + trackContext.effectiveDuration}")

            trackContext.track.setMarker(
                    TrackMarker(trackContext.startPosition + trackContext.effectiveDuration,
                            TrackEndMarkerHandler(this, trackContext)))
        }

        if (!silent && onPlayHook != null) onPlayHook!!.accept(trackContext)
    }

    internal open fun destroy() {
        stop()
        player.removeListener(this)
        player.link.destroy()
    }

    override fun onTrackException(player: AudioPlayer?, track: AudioTrack, exception: FriendlyException?) {
        log.error("Lavaplayer encountered an exception while playing {}",
                track.identifier, exception)
    }

    override fun onTrackStuck(player: AudioPlayer?, track: AudioTrack, thresholdMs: Long) {
        log.error("Lavaplayer got stuck while playing {}",
                track.identifier)
    }

    fun seekTo(position: Long) {
        if (context!!.track.isSeekable) {
            player.seekTo(position)
        } else {
            throw MessagingException(context!!.i18n("seekDeniedLiveTrack"))
        }
    }
}
