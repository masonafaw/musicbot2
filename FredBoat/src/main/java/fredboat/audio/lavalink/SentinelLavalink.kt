package fredboat.audio.lavalink

import com.fredboat.sentinel.entities.VoiceServerUpdate
import fredboat.config.idString
import fredboat.config.property.AppConfig
import fredboat.config.property.LavalinkConfig
import fredboat.sentinel.Guild
import fredboat.sentinel.Sentinel
import lavalink.client.io.Lavalink
import lavalink.client.io.metrics.LavalinkCollector
import org.json.JSONObject
import org.springframework.stereotype.Service

@Service
class SentinelLavalink(
        val sentinel: Sentinel,
        val appConfig: AppConfig,
        lavalinkConfig: LavalinkConfig
) : Lavalink<SentinelLink>(
        sentinel.selfUser.idString,
        appConfig.shardCount
) {

    companion object {
        lateinit var INSTANCE: SentinelLavalink
    }

    init {
        @Suppress("LeakingThis")
        INSTANCE = this
        lavalinkConfig.nodes.forEach { addNode(it.name, it.uri, it.password) }
        @Suppress("LeakingThis")
        LavalinkCollector(this).register<LavalinkCollector>()
    }

    override fun buildNewLink(guildId: String) = SentinelLink(this, guildId)

    fun getLink(guild: Guild) = getLink(guild.id.toString())
    fun getExistingLink(guild: Guild) = getExistingLink(guild.idString)

    fun onVoiceServerUpdate(update: VoiceServerUpdate) {
        val json = JSONObject(update.raw)
        val gId = json.getString("guild_id")
        val link = getLink(gId)

        link.onVoiceServerUpdate(json, update.sessionId)
    }
}