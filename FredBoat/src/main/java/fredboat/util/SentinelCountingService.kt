package fredboat.util

import com.fredboat.sentinel.entities.ExtendedShardInfo
import fredboat.config.property.AppConfig
import fredboat.sentinel.Sentinel
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.util.function.Tuple2

@Service
class SentinelCountingService(private val sentinel: Sentinel, appConfig: AppConfig) {

    /** Rough average users per shard + 5000 (for good measure) all timed the max number of shards */
    private val estimatedUsers = (30000 + 5000) * appConfig.shardCount

    private var cachedCounts: Counts = Counts(0,0,0,0,0,0, emptyList())
    private var cachedUserCount = 0
    private var countsCacheTime = 0L
    private var userCountCacheTime = 0L

    companion object {
        private const val COUNTS_TIMEOUT = 60000 // 1 minute
        private const val USERS_TIMEOUT = 10 * 60000 // 10 minutes
    }

    fun getCounts(): Mono<Counts> {
        if(countsCacheTime + COUNTS_TIMEOUT > System.currentTimeMillis()) return cachedCounts.toMono()

        return Mono.create { sink ->
            var guilds = 0L
            var roles = 0L
            var textChannels = 0L
            var voiceChannels = 0L
            var categories = 0L
            var emotes = 0L
            val shards = mutableListOf<ExtendedShardInfo>()

            sentinel.getAllSentinelInfo(includeShards = true)
                    .doOnComplete {
                        val result = Counts(guilds, roles, textChannels, voiceChannels, categories, emotes, shards)
                        cachedCounts = result
                        sink.success(result)
                    }
                    .doOnError { sink.error(it) }
                    .subscribe {
                        guilds += it.response.guilds
                        roles += it.response.roles
                        textChannels += it.response.textChannels
                        voiceChannels += it.response.voiceChannels
                        categories += it.response.categories
                        emotes += it.response.emotes
                        shards.addAll(it.response.shards!!)
                    }
        }
    }

    /**
     * The day that we reach 2,147,483,647 users will be a glorious one
     */
    fun getUniqueUserCount(): Mono<Int> {
        if(userCountCacheTime + USERS_TIMEOUT > System.currentTimeMillis()) return cachedUserCount.toMono()

        return Mono.create { sink ->
            val set = LongOpenHashSet(estimatedUsers)
            sentinel.getFullSentinelUserList()
                    .doOnComplete {
                        val sum = set.size
                        cachedUserCount = sum
                        sink.success(sum)
                    }.doOnError { sink.error(it) }
                    .subscribe { set.add(it) }
        }
    }

    fun getAllCounts(): Mono<Tuple2<Counts, Int>> = Mono.zip(getCounts(), getUniqueUserCount())

    fun getAllCountsCached() = cachedCounts to cachedUserCount

    data class Counts(
            val guilds: Long,
            val roles: Long,
            val textChannels: Long,
            val voiceChannels: Long,
            val categories: Long,
            val emotes: Long,
            val shards: List<ExtendedShardInfo>
    )
}