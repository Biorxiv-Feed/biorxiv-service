package com.biorxiv.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * {@link RestClient} for the bioRxiv API, built from Boot's auto-configured builder (Jackson converters,
 * timeouts from {@code spring.http.clients.*}) with the base URL from {@link BiorxivProperties}.
 */
@Configuration(proxyBeanMethods = false)
public class BiorxivClientConfig {

    @Bean
    public RestClient biorxivRestClient(RestClient.Builder builder, BiorxivProperties properties) {
        return builder.baseUrl(properties.api().baseUrl()).build();
    }
}
