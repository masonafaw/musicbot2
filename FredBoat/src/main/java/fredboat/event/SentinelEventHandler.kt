package fredboat.event

import com.fredboat.sentinel.entities.MessageReceivedEvent
import com.fredboat.sentinel.entities.ShardLifecycleEvent
import com.fredboat.sentinel.entities.ShardStatusChange
import com.fredboat.sentinel.entities.VoiceServerUpdate
import fredboat.sentinel.Guild
import fredboat.sentinel.Member
import fredboat.sentinel.User
import fredboat.sentinel.VoiceChannel
import java.time.Instant

/** Some events are only triggered when a guild is cached */
abstract class SentinelEventHandler {

    open fun onShardStatusChange(event: ShardStatusChange) {}
    open fun onShardLifecycle(event: ShardLifecycleEvent) {}

    open fun onGuildJoin(guild: Guild) {}
    open fun onGuildLeave(guildId: Long, joinTime: Instant) {}

    /** Only triggered for cached guilds */
    open fun onVoiceJoin(channel: VoiceChannel, member: Member) {}
    /** Only triggered for cached guilds */
    open fun onVoiceLeave(channel: VoiceChannel, member: Member) {}
    /** Only triggered for cached guilds */
    open fun onVoiceMove(oldChannel: VoiceChannel, newChannel: VoiceChannel, member: Member) {}
    open fun onVoiceServerUpdate(voiceServerUpdate: VoiceServerUpdate) {}

    open fun onGuildMessage(event: MessageReceivedEvent) {}
    open fun onGuildMessageDelete(guildId: Long, channelId: Long, messageId: Long) {}
    open fun onPrivateMessage(author: User, content: String) {}

}
