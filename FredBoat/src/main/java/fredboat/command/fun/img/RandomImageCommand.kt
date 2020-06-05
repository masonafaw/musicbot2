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

package fredboat.command.`fun`.img

import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IFunCommand
import fredboat.definitions.PermissionLevel
import fredboat.main.BotController
import fredboat.main.Launcher
import fredboat.messaging.internal.Context
import fredboat.perms.PermsUtil
import fredboat.util.rest.CacheUtil
import fredboat.util.rest.Http
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.lang.reflect.Array
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

open class RandomImageCommand(private val imgurAlbumUrl: String, name: String, vararg aliases: String) : Command(name, *aliases), IFunCommand {
    @Volatile
    private var etag: String? = ""

    //contains the images that this class randomly serves, default entry is a "my body is not ready" gif
    @Volatile
    private var urls = arrayOf("http://i.imgur.com/NqyOqnj.gif")

    companion object {
        private val log = LoggerFactory.getLogger(RandomImageCommand::class.java)
        private val imgurRefresher = Executors.newSingleThreadScheduledExecutor { runnable -> Thread(runnable, "imgur-refresher") }

        //https://regex101.com/r/0TDxsu/2
        private val IMGUR_ALBUM = Pattern.compile("^https?://imgur\\.com/a/([a-zA-Z0-9]+)$")
    }

    //Get a random image as a cached file
    val randomFile: File
        get() {
            val randomUrl: String = synchronized(this) { randomImageUrl }

            return CacheUtil.getImageFromURL(randomUrl)
        }

    val randomImageUrl: String
        get() = Array.get(urls, ThreadLocalRandom.current().nextInt(urls.size)) as String

    init {
        //update the album every hour
        imgurRefresher.scheduleAtFixedRate({
            try {
                populateItems(true)
            } catch (e: Exception) {
                log.error("Populating imgur album {} failed", imgurAlbumUrl, e)
            }
        }, 0, 1, TimeUnit.HOURS)
    }

    override suspend fun invoke(context: CommandContext) {
        if (context.rawArgs.contains("reload")) {
            if (PermsUtil.checkPermsWithFeedback(PermissionLevel.BOT_ADMIN, context)) {
                populateItems(false)
                context.reply("Reloaded imgur album.")
                return
            }
        }

        context.replyImage(randomImageUrl)
    }

    /**
     * Updates the imgur backed images managed by this object
     */
    private fun populateItems(useEtag: Boolean) {

        val m = IMGUR_ALBUM.matcher(this.imgurAlbumUrl)

        if (!m.find()) {
            log.error("Not a valid imgur album url {}", this.imgurAlbumUrl)
            return
        }

        val albumId = m.group(1)
        var request: Http.SimpleRequest = BotController.HTTP.get("https://api.imgur.com/3/album/$albumId")
                .auth("Client-ID " + Launcher.botController.credentials.imgurClientId)
        if (useEtag) {
            request = request.header("If-None-Match", etag!!)
        }

        try {
            request.execute().use { response ->
                //etag implementation: nothing has changed
                //https://api.imgur.com/performancetips
                //NOTE: imgur's implementation of this is wonky. occasionally they will return a new Etag without a
                // data change, and on the next fetch they will return the old Etag again.
                @Suppress("CascadeIf")
                if (response.code() == 304) {
                    //nothing to do here
                    log.info("Refreshed imgur album {}, no update.", this.imgurAlbumUrl)
                } else if (response.isSuccessful) {

                    val images = JSONObject(response.body()!!.string()).getJSONObject("data").getJSONArray("images")
                    val imageUrls = ArrayList<String>()
                    images.forEach { o -> imageUrls.add((o as JSONObject).getString("link")) }

                    synchronized(this) {
                        urls = imageUrls.toTypedArray()
                        etag = response.header("ETag")
                    }
                    log.info("Refreshed imgur album {}, new data found.", this.imgurAlbumUrl)
                } else {
                    //some other status

                    log.warn("Unexpected http status for imgur album request {}, response: {}\n{}",
                            this.imgurAlbumUrl, response.toString(), response.body()!!.string())
                }

            }
        } catch (e: IOException) {
            log.error("Imgur down? Could not fetch imgur album {}", this.imgurAlbumUrl, e)
        }

    }

    override fun help(context: Context): String {
        return "{0}{1}\n#Post a random image."
    }
}
