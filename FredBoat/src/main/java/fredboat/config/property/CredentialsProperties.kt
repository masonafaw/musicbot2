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

package fredboat.config.property

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.util.*

/**
 * Created by napster on 03.03.18.
 */
@Component
@ConfigurationProperties(prefix = "credentials")
class CredentialsProperties : Credentials {

    companion object {
        private val log = LoggerFactory.getLogger(CredentialsProperties::class.java)
    }

    private var discordBotToken = ""
    private var googleApiKeys: List<String> = ArrayList()
    private var imgurClientId = ""
    private var spotifyId = ""
    private var spotifySecret = ""
    private var openWeatherKey = ""
    private var sentryDsn = ""
    private var carbonKey = ""
    private var wastebinUser = ""
    private var wastebinPass = ""
    private var dikeUrl = ""

    override fun getBotToken(): String {
        if (discordBotToken.isBlank()) {
            val file = File("common.yml")
            if (file.exists()) {
                val yaml = Yaml()
                val map = yaml.load<Map<String, Any>>(FileInputStream(file))
                val newToken = map["discordToken"] as? String
                if (newToken != null) {
                    log.info("Discovered token in ${file.absolutePath}")
                    this.discordBotToken = newToken
                } else {
                    log.error("Found ${file.absolutePath} but no token!")
                }
            } else {
                log.warn("common.yml is missing and no token was defined by Spring")
            }
        }

        if (discordBotToken.isBlank()) {
            throw RuntimeException("No discord bot token provided." +
                    "\nMake sure to put a discord bot token into your common.yml file.")
        }

        return discordBotToken
    }
    override fun getGoogleKeys() = googleApiKeys
    override fun getImgurClientId() = imgurClientId
    override fun getSpotifyId() = spotifyId
    override fun getSpotifySecret() = spotifySecret
    override fun getOpenWeatherKey() = openWeatherKey
    override fun getSentryDsn() = sentryDsn
    override fun getCarbonKey() = carbonKey
    override fun getWastebinUser() = wastebinUser
    override fun getWastebinPass() = wastebinPass

    fun setDiscordBotToken(discordBotToken: String) {
        this.discordBotToken = discordBotToken
    }

    fun setGoogleApiKeys(googleApiKeys: List<String>) {
        this.googleApiKeys = googleApiKeys
        if (googleApiKeys.isEmpty()) {
            log.warn("No google API keys found. Some commands may not work, check the documentation.")
        }
    }

    fun setImgurClientId(imgurClientId: String) {
        this.imgurClientId = imgurClientId
    }

    fun setSpotifyId(spotifyId: String) {
        this.spotifyId = spotifyId
    }

    fun setSpotifySecret(spotifySecret: String) {
        this.spotifySecret = spotifySecret
    }

    fun setOpenWeatherKey(openWeatherKey: String) {
        this.openWeatherKey = openWeatherKey
    }

    fun setSentryDsn(sentryDsn: String) {
        this.sentryDsn = sentryDsn
    }

    fun setCarbonKey(carbonKey: String) {
        this.carbonKey = carbonKey
    }

    fun setWastebinUser(wastebinUser: String) {
        this.wastebinUser = wastebinUser
    }

    fun setWastebinPass(wastebinPass: String) {
        this.wastebinPass = wastebinPass
    }

    fun setDikeUrl(dikeUrl: String) {
        this.dikeUrl = dikeUrl
    }
}
