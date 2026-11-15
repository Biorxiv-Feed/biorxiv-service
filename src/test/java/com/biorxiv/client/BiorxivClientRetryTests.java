package com.biorxiv.client;

import com.biorxiv.client.impl.BiorxivClientImpl;
import com.biorxiv.config.BiorxivClientConfig;
import com.biorxiv.config.BiorxivProperties;
import com.biorxiv.config.SchedulingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;

/**
 * {@code @Retryable} on {@link BiorxivClientImpl#fetchPage}, with {@link SchedulingConfig} enabling it
 * and a 1 ms delay so the test doesn't wait for real backoff.
 * <p>
 * {@link AopAutoConfiguration} makes the slice proxy beans like the application does (class-based CGLIB proxies).
 * Without it the bean gets a JDK interface proxy, and Spring looks for {@code @Retryable} on the interface method
 * only, so the annotation on the implementation would be ignored and nothing would be retried.
 */
@RestClientTest(value = BiorxivClientImpl.class, properties = {
        "biorxiv.api.retry.max-retries=3",
        "biorxiv.api.retry.delay-ms=1",
        "biorxiv.api.retry.multiplier=1"})
@Import({BiorxivClientConfig.class, SchedulingConfig.class})
@ImportAutoConfiguration(AopAutoConfiguration.class)
@EnableConfigurationProperties(BiorxivProperties.class)
class BiorxivClientRetryTests {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 12);
    private static final String URL = "https://api.biorxiv.org/details/biorxiv/2026-11-12/2026-11-12/0/json";

    @Autowired
    private BiorxivClientImpl client;   // concrete type: only this bean is under test

    @Autowired
    private MockRestServiceServer server;

    @Test
    void transientFailuresAreRetriedUntilSuccess() {
        server.expect(once(), requestTo(URL)).andRespond(withServerError());
        server.expect(once(), requestTo(URL)).andRespond(withException(new IOException("connection reset")));
        server.expect(once(), requestTo(URL)).andRespond(withTooManyRequests());
        server.expect(once(), requestTo(URL))
                .andRespond(withSuccess(new ClassPathResource("biorxiv/details-page.json"), MediaType.APPLICATION_JSON));

        assertThat(client.fetchPage(DAY, DAY, 0).articles()).hasSize(2);
        server.verify();
    }

    @Test
    void givesUpAfterMaxRetries() {
        server.expect(times(4), requestTo(URL)).andRespond(withServerError());   // first attempt + 3 retries

        assertThatThrownBy(() -> client.fetchPage(DAY, DAY, 0)).isInstanceOf(HttpServerErrorException.class);
        server.verify();
    }

    @Test
    void clientErrorsAreNotRetried() {
        server.expect(once(), requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.fetchPage(DAY, DAY, 0)).isInstanceOf(HttpClientErrorException.NotFound.class);
        server.verify();
    }
}
