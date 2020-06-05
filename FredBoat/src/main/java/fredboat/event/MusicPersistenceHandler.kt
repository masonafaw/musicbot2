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

package fredboat.event

import com.fredboat.sentinel.entities.LifecycleEventEnum
import com.fredboat.sentinel.entities.SendMessageResponse
import com.fredboat.sentinel.entities.Shard
import com.fredboat.sentinel.entities.ShardLifecycleEvent
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import fredboat.audio.player.MusicTextChannelProvider
import fredboat.audio.player.PlayerRegistry
import fredboat.audio.queue.AudioTrackContext
import fredboat.audio.queue.SplitAudioTrackContext
import fredboat.config.property.AppConfig
import fredboat.config.property.Credentials
import fredboat.definitions.RepeatMode
import fredboat.feature.I18n
import fredboat.sentinel.getGuild
import fredboat.shared.constant.DistributionEnum
import fredboat.shared.constant.ExitCodes
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.FileUtils
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.text.MessageFormat
import java.time.Duration
import java.util.*
import java.util.function.BiConsumer

@Component
class MusicPersistenceHandler(private val playerRegistry: PlayerRegistry, private val credentials: Credentials,
                              private val musicTextChannelProvider: MusicTextChannelProvider,
                              @param:Qualifier("loadAudioPlayerManager") private val audioPlayerManager: AudioPlayerManager,
                              private val appConfig: AppConfig, private val allPlayerManagers: Set<AudioPlayerManager>
) : SentinelEventHandler() {

    companion object {
        private val log = LoggerFactory.getLogger(MusicPersistenceHandler::class.java)
    }

    //TODO this needs to happen before the shard manager is shut down, inside of a shutdown hook (so shutdown signals are properly processed)
    fun handlePreShutdown(code: Int) {
        if (!appConfig.isMusicDistribution) {
            val announcements = announceAndPersist(code)

            // this makes sure that the announcements actually reach the users. if we go into full shut down before
            // that, JDA's requester may not deliver all announcements
            for (announcement in announcements) {
                try {
                    announcement.block(Duration.ofSeconds(30)) //30 seconds is enough on patron boat, we don't announce on public boat (music distribution)
                } catch (ignored: Exception) {
                }

            }
        }

        //will also shutdown all AudioSourceManagers registered with the AudioPlayerManagers
        for (playerManager in allPlayerManagers) {
            playerManager.shutdown()
        }
    }

    /**
     * @return a list of futures that will completed as soon as we sent out all announcements to users about the shutdown
     */
    private fun announceAndPersist(code: Int): MutableList<Mono<SendMessageResponse>> {
        val dir = File("music_persistence")
        if (!dir.exists()) {
            val created = dir.mkdir()
            if (!created) {
                log.error("Failed to create music persistence directory")
                return emptyList<Mono<SendMessageResponse>>().toMutableList()
            }
        }

        val isUpdate = code == ExitCodes.EXIT_CODE_UPDATE
        val isRestart = code == ExitCodes.EXIT_CODE_RESTART

        val announcements = mutableListOf<Mono<SendMessageResponse>>()
        playerRegistry.forEach(BiConsumer { guildId, player ->
            try {

                val msg: String = when {
                    isUpdate -> I18n.get(player.guild).getString("shutdownUpdating")
                    isRestart -> I18n.get(player.guild).getString("shutdownRestarting")
                    else -> I18n.get(player.guild).getString("shutdownIndef")
                }

                val activeTextChannel = player.activeTextChannel
                if (activeTextChannel != null && player.isPlaying) {
                    announcements.add(activeTextChannel.send(msg))
                }

                val data = JSONObject()
                val vc = player.currentVoiceChannel
                data.put("vc", vc?.id ?: 0)
                data.put("tc", activeTextChannel?.id ?: 0)
                data.put("isPaused", player.isPaused)
                data.put("volume", player.volume.toString())
                data.put("repeatMode", player.repeatMode)
                data.put("shuffle", player.isShuffle)

                if (player.playingTrack != null) {
                    data.put("position", player.position)
                }

                val identifiers = ArrayList<JSONObject>()

                for (atc in player.remainingTracks) {
                    val baos = ByteArrayOutputStream()
                    audioPlayerManager.encodeTrack(MessageOutput(baos), atc.track)

                    val ident = JSONObject()
                            .put("message", Base64.encodeBase64String(baos.toByteArray()))
                            .put("user", atc.userId)

                    if (atc is SplitAudioTrackContext) {
                        val split = JSONObject()
                        split.put("title", atc.effectiveTitle)
                                .put("startPos", atc.startPosition)
                                .put("endPos", atc.startPosition + atc.effectiveDuration)

                        ident.put("split", split)
                    }

                    identifiers.add(ident)
                }

                data.put("sources", identifiers)

                try {
                    FileUtils.writeStringToFile(
                            File(dir, guildId.toString()),
                            data.toString(),
                            Charset.forName("UTF-8")
                    )
                } catch (ex: IOException) {
                    activeTextChannel?.send(MessageFormat.format(
                            I18n.get(player.guild).getString("shutdownPersistenceFail"),
                            ex.message
                    ))?.subscribe()
                }

            } catch (ex: Exception) {
                log.error("Error when saving persistence file", ex)
            }
        })

        return announcements
    }

    override fun onShardLifecycle(event: ShardLifecycleEvent) {
        if (event.change != LifecycleEventEnum.READIED) return

        //the current implementation of music persistence is not a good idea on big bots
        if (appConfig.shardCount <= 10 && appConfig.distribution != DistributionEnum.MUSIC) {
            GlobalScope.launch {
                try {
                    reloadPlaylists(event.shard)
                } catch (e: Exception) {
                    log.error("Uncaught exception when dispatching ready event to music persistence handler", e)
                }
            }
        }
    }

    private suspend fun reloadPlaylists(shard: Shard) {
        val dir = File("music_persistence")

        if (appConfig.isMusicDistribution) {
            log.warn("Music persistence loading is disabled on the MUSIC distribution! Use PATRON or DEVELOPMENT instead" + "How did this call end up in here anyways?")
            return
        }

        log.info("Began reloading playlists for shard {}", shard)
        if (!dir.exists()) {
            log.info("No music persistence directory found.")
            return
        }
        val files = dir.listFiles()
        if (files == null || files.isEmpty()) {
            log.info("No files present in music persistence directory")
            return
        }

        for (file in files) {
            try {
                val guild = getGuild(file.name.toLong()) ?: continue

                if (guild.shardId != shard.id || !guild.selfPresent) continue

                val data = JSONObject(FileUtils.readFileToString(file, Charset.forName("UTF-8")))

                val isPaused = data.getBoolean("isPaused")
                val sources = data.getJSONArray("sources")
                val vc = guild.getVoiceChannel(data.getLong("vc"))
                val tc = guild.getTextChannel(data.getLong("tc"))
                val volume = data.getString("volume").toFloat()
                val repeatMode = data.getEnum(RepeatMode::class.java, "repeatMode")
                val shuffle = data.getBoolean("shuffle")

                val player = playerRegistry.getOrCreate(guild)

                if (tc != null) {
                    musicTextChannelProvider.setMusicChannel(tc)
                }
                if (appConfig.distribution.volumeSupported()) {
                    player.volume = volume
                }
                player.repeatMode = repeatMode
                player.isShuffle = shuffle

                val isFirst = booleanArrayOf(true)

                val tracks = ArrayList<AudioTrackContext>()
                sources.forEach { t: Any ->
                    val json = t as JSONObject
                    val message = Base64.decodeBase64(json.getString("message"))
                    val member = guild.getMember(json.getLong("user")) ?: guild.selfMember
                    //The member may have left the guild meanwhile, so we may set ourselves as the one who added the song

                    val at: AudioTrack?
                    try {
                        val bais = ByteArrayInputStream(message)
                        at = audioPlayerManager.decodeTrack(MessageInput(bais)).decodedTrack
                    } catch (e: IOException) {
                        throw RuntimeException(e)
                    }

                    if (at == null) {
                        log.error("Loaded track that was null! Skipping...")
                        return@forEach
                    }

                    // Handle split tracks
                    val atc: AudioTrackContext
                    val split = json.optJSONObject("split")
                    if (split != null) {
                        atc = SplitAudioTrackContext(at, member,
                                split.getLong("startPos"),
                                split.getLong("endPos"),
                                split.getString("title")
                        )
                        at.position = split.getLong("startPos")

                        if (isFirst[0]) {
                            isFirst[0] = false
                            if (data.has("position")) {
                                at.position = split.getLong("startPos") + data.getLong("position")
                            }
                        }
                    } else {
                        atc = AudioTrackContext(at, member)

                        if (isFirst[0]) {
                            isFirst[0] = false
                            if (data.has("position")) {
                                at.position = data.getLong("position")
                            }
                        }
                    }

                    tracks.add(atc)
                }

                player.loadAll(tracks)
                if (!isPaused) {
                    if (vc != null) {
                        GlobalScope.launch {
                            try {
                                player.joinChannel(vc)
                                player.play()
                            } catch (ignored: Exception) {
                            }
                        }
                    }
                    tc?.send(MessageFormat.format(I18n.get(guild).getString("reloadSuccess"), sources.length()))
                            ?.subscribe()
                }
            } catch (ex: Exception) {
                log.error("Error when loading persistence file", ex)
            }

            val deleted = file.delete()
            log.info(if (deleted) "Deleted persistence file: $file" else "Failed to delete persistence file: $file")
        }
    }

}
