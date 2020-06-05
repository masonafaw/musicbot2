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

package fredboat.agent

import com.fredboat.sentinel.entities.ShardStatus
import fredboat.config.property.AppConfig
import fredboat.config.property.Credentials
import fredboat.main.BotController
import fredboat.util.SentinelCountingService
import fredboat.util.rest.Http
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@Service
class CarbonitexAgent(
        private val credentials: Credentials,
        private val appConfig: AppConfig,
        private val counting: SentinelCountingService
) : FredBoatAgent("carbonitex", 30, TimeUnit.MINUTES) {

    public override fun doRun() {
        GlobalScope.launch {
            sendStats()
        }
    }

    private suspend fun sendStats() {
        val allConnected = AtomicBoolean(true)
        val shardCounter = AtomicInteger(0)
        val counts = counting.getCounts().awaitSingle()
        counts.shards.forEach { shard ->
            shardCounter.incrementAndGet()
            if (shard.shard.status != ShardStatus.CONNECTED) {
                allConnected.set(false)
            }
        }
        if (!allConnected.get()) {
            log.warn("Skipping posting stats because not all shards are online!")
            return
        }

        if (shardCounter.get() < appConfig.shardCount) {
            log.warn("Skipping posting stats because not all shards initialized!")
            return
        }

        try {
            BotController.HTTP.post("https://www.carbonitex.net/discord/data/botdata.php",
                    Http.Params.of(
                            "key", credentials.carbonKey,
                            "servercount", counts.guilds.toString()
                    ))
                    .execute().use { response ->


                        val content = response.body()!!.string()
                        if (response.isSuccessful) {
                            log.info("Successfully posted the bot data to carbonitex.com: {}", content)
                        } else {
                            log.warn("Failed to post stats to Carbonitex: {}\n{}", response.toString(), content)
                        }
                    }
        } catch (e: Exception) {
            log.error("An error occurred while posting the bot data to carbonitex.com", e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(CarbonitexAgent::class.java)
    }

}
