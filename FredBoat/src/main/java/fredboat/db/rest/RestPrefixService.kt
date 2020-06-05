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

package fredboat.db.rest

import fredboat.config.property.BackendConfig
import fredboat.db.FriendlyEntityService.fetchUserFriendly
import fredboat.db.api.PrefixService
import fredboat.db.transfer.Prefix
import fredboat.sentinel.Guild
import fredboat.sentinel.RawUser
import io.prometheus.client.guava.cache.CacheMetricsCollector
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.util.*
import java.util.function.Function


/**
 * Created by napster on 17.02.18.
 */
@Component
class RestPrefixService(
        @param:Qualifier("selfUser")
        private val selfUser: RawUser,
        backendConfig: BackendConfig,
        quarterdeckRestTemplate: RestTemplate,
        cacheMetrics: CacheMetricsCollector
) : CachedRestService<Prefix.GuildBotId, Prefix>(
        backendConfig.quarterdeck.host + RestService.VERSION_PATH + PATH,
        Prefix::class.java,
        quarterdeckRestTemplate,
        cacheMetrics,
        RestPrefixService::class.java.simpleName
), PrefixService {

    companion object {
        const val PATH = "prefix/"
    }

    override fun transformPrefix(guild: Guild, transformation: Function<Prefix, Prefix>): Prefix {
        val prefix = fetchUserFriendly { fetch(Prefix.GuildBotId(guild, selfUser.id)) }
        return fetchUserFriendly { merge(transformation.apply(prefix)) }
    }

    override fun getPrefix(id: Prefix.GuildBotId): Optional<String> {
        try {
            return Optional.ofNullable(backendRestTemplate.postForObject(path + "getraw", id, String::class.java))
        } catch (e: RestClientException) {
            throw BackendException("Could not get prefix for guild " + id.guildId, e)
        }

    }
}
