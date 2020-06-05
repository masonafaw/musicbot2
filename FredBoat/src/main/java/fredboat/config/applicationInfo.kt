package fredboat.config

import fredboat.config.property.Credentials
import fredboat.sentinel.RawUser
import fredboat.util.rest.Http
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


private val log: Logger = LoggerFactory.getLogger(ApplicationInfo::class.java)
private const val APP_URL = "https://discordapp.com/api/v6/oauth2/applications/@me"
private const val USER_URL = "https://discordapp.com/api/v6/users/@me"

@Configuration
class DiscordInfoProvider{
    @Bean
    @ConditionalOnMissingClass("fredboat.testutil.Flag")
    fun applicationInfo(credentials: Credentials): ApplicationInfo {
        log.info("Retrieving application info")
        Http(Http.DEFAULT_BUILDER).get(APP_URL)
                .header("Authorization", "Bot " + credentials.botToken)
                .asJson()
                .run {
                    return ApplicationInfo(
                            getLong("id"),
                            getBoolean("bot_require_code_grant"),
                            optString("description") ?: "",
                            optString("icon") ?: "",
                            getString("name"),
                            getJSONObject("owner").getLong("id"),
                            getBoolean("bot_public")
                    )
                }
    }

    @Bean
    @ConditionalOnMissingClass("fredboat.testutil.Flag")
    fun selfUser(credentials: Credentials): RawUser {
        log.info("Retrieving self user info")
        Http(Http.DEFAULT_BUILDER).get(USER_URL)
                .header("Authorization", "Bot " + credentials.botToken)
                .asJson()
                .run {
                    return RawUser(
                            getLong("id"),
                            getString("username"),
                            getString("discriminator"),
                            getBoolean("bot")
                    )
                }
    }
}

data class ApplicationInfo(
        val id: Long,
        val requiresCodeGrant: Boolean,
        val description: String,
        val iconId: String,
        val name: String,
        val ownerId: Long,
        val public: Boolean
)

val RawUser.idString get() = id.toString()