package fredboat.event

import fredboat.audio.player.PlayerRegistry
import fredboat.command.info.HelloCommand
import fredboat.db.api.GuildDataService
import fredboat.feature.metrics.Metrics
import fredboat.sentinel.Guild
import fredboat.sentinel.TextChannel
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant

@Component
class GuildEventHandler(
        private val guildDataService: GuildDataService,
        private val playerRegistry: PlayerRegistry
) : SentinelEventHandler() {
    override fun onGuildJoin(guild: Guild) {
        // Wait a few seconds to allow permissions to be set and applied and propagated
        val mono = Mono.create<Unit> {
            sendHelloOnJoin(guild)
        }
        mono.delaySubscription(Duration.ofSeconds(10))
                .subscribe()
    }

    override fun onGuildLeave(guildId: Long, joinTime: Instant) {
        playerRegistry.destroyPlayer(guildId)

        val lifespan = Instant.now().epochSecond - joinTime.epochSecond
        Metrics.guildLifespan.observe(lifespan.toDouble())
    }

    private fun sendHelloOnJoin(guild: Guild) {
        //filter guilds that already received a hello message
        // useful for when discord trolls us with fake guild joins
        // or to prevent it send repeatedly due to kick and reinvite
        val gd = guildDataService.fetchGuildData(guild)
        if (gd.timestampHelloSent > 0) {
            return
        }

        var channel: TextChannel? = guild.getTextChannel(guild.id) //old public channel
        if (channel == null || !channel.canTalk()) {
            //find first channel that we can talk in
            guild.textChannels.forEach { _, tc ->
                if (tc.canTalk()) {
                    channel = tc
                    return@forEach
                }
            }
        }

        //send actual hello message and persist on success
        channel?.send(HelloCommand.getHello(guild))
                ?.doOnSuccess { guildDataService.transformGuildData(guild, { it.helloSent() }) }
                ?.subscribe()
    }
}