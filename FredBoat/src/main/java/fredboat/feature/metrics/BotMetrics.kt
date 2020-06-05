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

package fredboat.feature.metrics

import fredboat.agent.StatsAgent
import fredboat.audio.player.PlayerRegistry
import fredboat.sentinel.RawUser
import fredboat.sentinel.Sentinel
import fredboat.util.DiscordUtil
import fredboat.util.SentinelCountingService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import reactor.util.function.Tuple2

/**
 * Metrics for the whole FredBoat.
 *
 * Sets up stats actions for the various metrics that we want to count proactively on our own terms instead of whenever
 * Prometheus scrapes.
 */
@Service
class BotMetrics(
        private val statsAgent: StatsAgent,
        @param:Qualifier("selfUser")
        private val selfUser: RawUser,
        private val playerRegistry: PlayerRegistry,
        private val sentinel: Sentinel,
        private val sentinelCountingService: SentinelCountingService
) {
    val dockerStats = DockerStats()
    val musicPlayerStats = MusicPlayerStats()
    final var sentinelInfo: List<Sentinel.NamedSentinelInfoResponse> = emptyList()
        private set
    final var entityCounts: Tuple2<SentinelCountingService.Counts, Int>? = null

    init {
        start()
    }

    private fun start() {
        if (DiscordUtil.isOfficialBot(selfUser.id)) {
            statsAgent.addAction(StatsAgent.ActionAdapter("docker stats for fredboat") {
                dockerStats.fetch()
            })
        }

        statsAgent.addAction(StatsAgent.ActionAdapter("music player stats for fredboat") {
            musicPlayerStats.count(playerRegistry)
        })

        statsAgent.addAction(StatsAgent.ActionAdapter("sentinel info including shard status", intervalMinutes = 1) {
            sentinel.getAllSentinelInfo(includeShards = true)
                    .collectList()
                    .subscribe { sentinelInfo = it }
        })

        statsAgent.addAction(StatsAgent.ActionAdapter("entity counts") {
            sentinelCountingService.getAllCounts()
                    .subscribe { entityCounts = it }
        })
    }
}
