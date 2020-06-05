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

package fredboat.config;

import fredboat.config.property.BackendConfig;
import fredboat.feature.metrics.Metrics;
import fredboat.metrics.OkHttpEventMetrics;
import fredboat.util.rest.Http;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * Created by napster on 16.03.18.
 */
@Configuration
public class RestTemplateConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateConfiguration.class);

    @Bean
    public GsonHttpMessageConverter gsonHttpMessageConverter() {
        return new GsonHttpMessageConverter();
    }

    @Bean
    public RestTemplate quarterdeckRestTemplate(BackendConfig backendConfig, GsonHttpMessageConverter gson) {
        return new RestTemplateBuilder()
                .basicAuthorization(backendConfig.getQuarterdeck().getUser(), backendConfig.getQuarterdeck().getPass())
                .requestFactory(this::clientHttpRequestFactory)
                .messageConverters(gson)
                .additionalInterceptors(((req, body, execution) -> {
                    String method = req.getMethodValue();
                    String uri = req.getURI().toString();
                    log.debug(">>>{} {} {}", method, uri, new String(body));
                    ClientHttpResponse response = execution.execute(req, body);
                    if (response.getBody().markSupported()) { //this is true if the buffering http request factory is used
                        response.getBody().mark(Integer.MAX_VALUE);
                        log.debug("<<<{} {} {}", method, uri, IOUtils.toString(response.getBody(), "UTF-8"));
                        response.getBody().reset();
                    }
                    return response;
                }))
                .build();
    }

    /**
     * @return a ClientHttpRequestFactory to use with our quarterdeck rest template. It will be a buffering one if debug
     * logs are enabled, so that we can read the body of the respone more than once and log it.
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        ClientHttpRequestFactory requestFactory = new OkHttp3ClientHttpRequestFactory(Http.DEFAULT_BUILDER
                .newBuilder()
                .eventListener(new OkHttpEventMetrics("quarterdeck", Metrics.httpEventCounter))
                .build());

        if (log.isDebugEnabled()) {
            requestFactory = new BufferingClientHttpRequestFactory(requestFactory);
        }
        return requestFactory;
    }
}
