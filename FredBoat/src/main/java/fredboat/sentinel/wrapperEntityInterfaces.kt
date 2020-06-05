package fredboat.sentinel

import fredboat.perms.IPermissionSet
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.*

private const val DISCORD_EPOCH = 1420070400000L

@Suppress("unused")
@Service
class SentinelProvider(sentinelParam: Sentinel) {
    init {
        providedSentinel = sentinelParam
    }
}

private lateinit var providedSentinel: Sentinel

interface SentinelEntity {
    val id: Long
    val idString: String
        get() = id.toString()
    val sentinel: Sentinel
        get() = providedSentinel
    val creationTime: OffsetDateTime
        get() {
            // https://discordapp.com/developers/docs/reference#convert-snowflake-to-datetime
            // Shift 22 bits right so we only have the time
            val timestamp = (id ushr 22) + DISCORD_EPOCH
            val gmt = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
            gmt.timeInMillis = timestamp
            return OffsetDateTime.ofInstant(gmt.toInstant(), gmt.timeZone.toZoneId())
        }
}

interface Channel : SentinelEntity {
    override val id: Long
    val name: String
    val guild: Guild
    val ourEffectivePermissions: IPermissionSet
    fun checkOurPermissions(permissions: IPermissionSet) = ourEffectivePermissions has permissions
}

interface IMentionable {
    val asMention: String
}