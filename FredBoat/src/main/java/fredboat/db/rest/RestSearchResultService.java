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
import fredboat.db.api.SearchResultService;
import fredboat.db.transfer.SearchResult;
import io.prometheus.client.guava.cache.CacheMetricsCollector;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * Created by napster on 17.02.18.
 */
@Component
public class RestSearchResultService extends CachedRestService<SearchResult.SearchResultId, SearchResult> implements SearchResultService {

    public static final String PATH = "searchresult/";

    public RestSearchResultService(BackendConfig backendConfig, RestTemplate quarterdeckRestTemplate,
                                   CacheMetricsCollector cacheMetrics) {
        super(backendConfig.getQuarterdeck().getHost() + VERSION_PATH + PATH, SearchResult.class,
                quarterdeckRestTemplate, cacheMetrics, RestSearchResultService.class.getSimpleName());
    }

    /**
     * Merge a search result into the database.
     *
     * @return the merged SearchResult object, or null when there is no cache database
     */
    @Override
    public Optional<SearchResult> mergeSearchResult(SearchResult searchResult) {
        try {
            return Optional.of(merge(searchResult));
        } catch (Exception e) {
            log.error("Could not merge search result for " + searchResult.getId(), e);
            return Optional.empty();
        }
    }

    /**
     * @param maxAgeMillis the maximum age of the cached search result; provide a negative value for eternal cache
     * @return the cached search result; may return null for a non-existing or outdated search, or when there is no
     * cache database
     */
    @Override
    public Optional<SearchResult> getSearchResult(SearchResult.SearchResultId id, long maxAgeMillis) {
        try {
            return Optional.ofNullable(backendRestTemplate.postForObject(path + "getmaxaged?millis={millis}", id,
                    SearchResult.class, Long.toString(maxAgeMillis)));
        } catch (Exception e) {
            log.error("Could not get search result for " + id, e);
            return Optional.empty();
        }
    }
}
