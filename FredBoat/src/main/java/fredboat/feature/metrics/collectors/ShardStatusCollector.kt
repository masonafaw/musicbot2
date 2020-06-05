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

package fredboat.feature.metrics.collectors

import com.fredboat.sentinel.entities.ShardStatus
import fredboat.config.property.AppConfig
import fredboat.feature.metrics.BotMetrics
import io.prometheus.client.Collector
import io.prometheus.client.GaugeMetricFamily
import org.springframework.stereotype.Component
import java.util.*

/**
 * Created by napster on 19.04.18.
 */
@Component
class ShardStatusCollector(private val botMetrics: BotMetrics, private val appConfig: AppConfig) : Collector() {

    override fun collect(): List<Collector.MetricFamilySamples> {
        val mfs = ArrayList<Collector.MetricFamilySamples>()
        val noLabels = emptyList<String>()

        val totalShards = GaugeMetricFamily("fredboat_shards_total",
                "Total shards managed by this instance", noLabels)
        totalShards.addMetric(noLabels, appConfig.shardCount.toDouble())
        mfs.add(totalShards)

        val shardStatus = GaugeMetricFamily(
                "fredboat_shard_status",
                "JDA status of shards, according to Sentinels",
                listOf("status")
        )

        val counts = mutableMapOf<ShardStatus, Int>()
        ShardStatus.values().forEach { counts[it] = 0 }
        botMetrics.sentinelInfo.flatMap { it.response.shards!! }
                .forEach { counts[it.shard.status] = counts[it.shard.status]!! + 1 }
        counts.forEach { status, count ->
            shardStatus.addMetric(listOf(status.name), count.toDouble())
        }
        mfs.add(shardStatus)

        return mfs
    }
}