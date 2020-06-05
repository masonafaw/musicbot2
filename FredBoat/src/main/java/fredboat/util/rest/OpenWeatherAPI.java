package fredboat.util.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import fredboat.config.property.Credentials;
import fredboat.feature.metrics.Metrics;
import fredboat.metrics.OkHttpEventMetrics;
import fredboat.util.rest.models.weather.OpenWeatherCurrent;
import fredboat.util.rest.models.weather.RetrievedWeather;
import fredboat.util.rest.models.weather.WeatherError;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import io.prometheus.client.guava.cache.CacheMetricsCollector;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
public class OpenWeatherAPI implements Weather {
    private static final String TAG = "OpenWeather";
    private static final Logger log = LoggerFactory.getLogger(OpenWeatherAPI.class);
    private static final String OPEN_WEATHER_BASE_URL = "https://api.openweathermap.org/data/2.5";
    private static final int MAX_CACHE_HOUR = 6;
    private static final int MAX_API_RATE = 60;
    private final Credentials credentials;
    private LoadingCache<String, RetrievedWeather> weatherCache;

    private final Bucket limitBucket;
    protected OkHttpClient client;
    private ObjectMapper objectMapper;
    private HttpUrl currentWeatherBaseUrl;

    public OpenWeatherAPI(CacheMetricsCollector cacheMetrics, Credentials credentials) {
        this.credentials = credentials;
        client = Http.DEFAULT_BUILDER.newBuilder()
                .eventListener(new OkHttpEventMetrics("openWeatherApi", Metrics.httpEventCounter))
                .build();
        objectMapper = new ObjectMapper();

        currentWeatherBaseUrl = HttpUrl.parse(OPEN_WEATHER_BASE_URL + "/weather");
        if (currentWeatherBaseUrl == null) {
            log.debug("Open weather search unable to build URL");
        }

        Refill refill = Refill.smooth(MAX_API_RATE, Duration.ofSeconds(60));
        Bandwidth limit = Bandwidth.classic(MAX_API_RATE, refill);
        limitBucket = Bucket4j.builder().addLimit(limit).build();

        weatherCache = CacheBuilder.newBuilder()
                .recordStats()
                .expireAfterWrite(MAX_CACHE_HOUR, TimeUnit.HOURS)
                .build(new CacheLoader<String, RetrievedWeather>() {
                    @Override
                    public RetrievedWeather load(@Nonnull String key) {
                        return processGetWeatherByCity(key);
                    }
                });
        cacheMetrics.addCache("openWeatherApi", weatherCache);

        testOpenWeatherKey();
    }

    /**
     * Method to check if there is an error to retrieve open weather data.
     */
    private void testOpenWeatherKey() {
        if ("".equals(credentials.getOpenWeatherKey())) {
            log.warn("Open Weather API credentials not found. Weather related commands will not work properly.");
            return;
        }

        RetrievedWeather weather = getCurrentWeatherByCity("san francisco");

        boolean isSuccess = !(weather == null || weather.isError());

        if (isSuccess) {
            log.info("Open Weather API check successful");
        } else {
            log.warn("Open Weather API check failed. It may be down, the provided credentials may be invalid, or temporarily blocked.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RetrievedWeather getCurrentWeatherByCity(@Nonnull String query) throws APILimitException {
        try {

            return weatherCache.get(query);
        } catch (ExecutionException e) {
            log.error(e.getMessage());
            Throwables.propagateIfPossible(
                    e.getCause(), APILimitException.class);

            // Will never run.
            throw new IllegalStateException(e);

        } catch (UncheckedExecutionException e) {
            log.error(e.getMessage());
            Throwables.throwIfUnchecked(e.getCause());

            // Will never run.
            throw new IllegalStateException(e);
        }

    }

    /**
     * Weather retrieving method.
     *
     * @param query String to query the weather.
     * @return Null if there is an error or retrieved weather.
     */
    private RetrievedWeather processGetWeatherByCity(@Nonnull String query) throws APILimitException {
        RetrievedWeather retrievedWeather = null;

        if (currentWeatherBaseUrl != null && query.length() > 0) {

            // Check if rate is exceeded.
            if (limitBucket.tryConsume(1)) {
                log.info("Retrieving " + query + " without cache.");
                HttpUrl.Builder urlBuilder = currentWeatherBaseUrl.newBuilder();

                urlBuilder.addQueryParameter("q", query);
                urlBuilder.addQueryParameter("appid", credentials.getOpenWeatherKey());

                HttpUrl url = urlBuilder.build();
                Request request = new Request.Builder()
                        .url(url)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    ResponseBody responseBody = response.body();

                    switch (response.code()) {
                        case 200:
                            if (responseBody != null) {
                                retrievedWeather = objectMapper.readValue(responseBody.string(), OpenWeatherCurrent.class);
                            }
                            break;
                            
                        case 400:
                        case 404:
                            retrievedWeather = new WeatherError(RetrievedWeather.ErrorCode.LOCATION_NOT_FOUND);
                            break;

                        default:
                            log.warn(TAG + " search error status code " + response.code());
                            if (responseBody != null) {
                                log.warn(responseBody.string());
                            }
                            retrievedWeather = new WeatherError(RetrievedWeather.ErrorCode.SOMETHING_IS_WRONG);
                            break;
                    }
                } catch (IOException e) {
                    log.warn(TAG + " search: ", e);
                    retrievedWeather = new WeatherError(RetrievedWeather.ErrorCode.SOMETHING_IS_WRONG);
                }
            } else {
                throw new APILimitException(TAG + " API maximum rate exceeded.");
            }
        }

        if (retrievedWeather == null) {
            retrievedWeather = new WeatherError(RetrievedWeather.ErrorCode.SOMETHING_IS_WRONG);
        }
        return retrievedWeather;
    }
}
