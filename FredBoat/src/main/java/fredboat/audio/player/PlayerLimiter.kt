package fredboat.audio.player

import fredboat.commandmeta.abs.CommandContext
import fredboat.config.property.AppConfig
import fredboat.sentinel.Guild
import fredboat.shared.constant.BotConstants
import org.springframework.stereotype.Component

@Component
class PlayerLimiter(appConfig: AppConfig) {

    // A negative limit means unlimited
    val limit: Int = appConfig.playerLimit

    private fun checkLimit(guild: Guild, playerRegistry: PlayerRegistry): Boolean {
        val guildPlayer = playerRegistry.getExisting(guild)

        return if (guildPlayer != null && guildPlayer.trackCount > 0) true else
            limit < 0 || playerRegistry.playingCount() < limit
    }

    fun checkLimitResponsive(context: CommandContext, playerRegistry: PlayerRegistry): Boolean {
        val b = checkLimit(context.guild, playerRegistry)

        if (!b) {
            val patronUrl = "<" + BotConstants.DOCS_DONATE_URL + ">"
            val msg = context.i18nFormat("playersLimited", limit, patronUrl)
            context.reply(msg)
        }

        return b
    }
}
