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

package fredboat.api

import fredboat.audio.player.PlayerRegistry
import fredboat.main.getBotController
import fredboat.util.SentinelCountingService
import org.json.JSONArray
import org.json.JSONObject
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/stats")
class Stats(
        private val sentinelCountingService: SentinelCountingService,
        private val playerRegistry: PlayerRegistry
) {

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getStats(): Mono<String> {

        lateinit var counts: SentinelCountingService.Counts
        var uniqueUsersCount = -1
        val countsMono = sentinelCountingService.getCounts().doOnSuccess { counts = it }
        val usersMono = sentinelCountingService.getUniqueUserCount().doOnSuccess { uniqueUsersCount = it }

        val responseMono = Mono.create<String> {
            val root = JSONObject()
            val a = JSONArray()

            counts.shards.forEach { shard ->
                val fbStats = JSONObject()
                fbStats.put("id", shard.shard.id)
                        .put("guilds", shard.guilds)
                        .put("users", shard.users)
                        .put("status", shard.shard.status)

                a.put(fbStats)
            }

            val g = JSONObject()
            g.put("playingPlayers", playerRegistry.playingCount())
                    .put("totalPlayers", playerRegistry.totalCount())
                    .put("distribution", getBotController().appConfig.distribution)
                    .put("guilds", counts.guilds)
                    .put("users", uniqueUsersCount)

            root.put("shards", a)
            root.put("global", g)

            it.success(root.toString())
        }

        return countsMono.and(usersMono).then(responseMono)
    }
}
