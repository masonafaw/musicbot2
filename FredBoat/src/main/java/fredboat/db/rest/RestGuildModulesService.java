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

package fredboat.db.rest;

import fredboat.config.property.BackendConfig;
import fredboat.db.api.GuildModulesService;
import fredboat.db.transfer.GuildModules;
import fredboat.sentinel.Guild;
import io.prometheus.client.guava.cache.CacheMetricsCollector;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.function.Function;

import static fredboat.db.FriendlyEntityService.fetchUserFriendly;

/**
 * Created by napster on 17.02.18.
 */
@Component
public class RestGuildModulesService extends CachedRestService<Long, GuildModules> implements GuildModulesService {

    public static final String PATH = "guildmodules/";

    public RestGuildModulesService(BackendConfig backendConfig, RestTemplate quarterdeckRestTemplate,
                                   CacheMetricsCollector cacheMetrics) {
        super(backendConfig.getQuarterdeck().getHost() + VERSION_PATH + PATH, GuildModules.class,
                quarterdeckRestTemplate, cacheMetrics, RestGuildModulesService.class.getSimpleName());
    }

    @Override
    public GuildModules fetchGuildModules(Guild guild) {
        return fetchUserFriendly(() -> fetch(guild.getId()));
    }

    @Override
    public GuildModules transformGuildModules(Guild guild, Function<GuildModules, GuildModules> transformation) {
        return fetchUserFriendly(() -> merge(transformation.apply(fetchGuildModules(guild))));
    }
}
