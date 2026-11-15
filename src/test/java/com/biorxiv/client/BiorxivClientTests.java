package com.biorxiv.client;

import com.biorxiv.client.dto.BiorxivArticleDto;
import com.biorxiv.client.impl.BiorxivClientImpl;
import com.biorxiv.config.BiorxivClientConfig;
import com.biorxiv.config.BiorxivProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * HTTP and JSON handling against recorded API responses. {@code @Retryable} is not active in this slice
 * ({@code SchedulingConfig} is not loaded), so a server error surfaces immediately.
 */
@RestClientTest(BiorxivClientImpl.class)
@Import(BiorxivClientConfig.class)
@EnableConfigurationProperties(BiorxivProperties.class)
class BiorxivClientTests {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 12);

    @Autowired
    private BiorxivClient client;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void parsesPageWithStringNumbersAndUnknownFields() {
        server.expect(requestTo("https://api.biorxiv.org/details/biorxiv/2026-11-12/2026-11-12/30/json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ClassPathResource("biorxiv/details-page.json"), MediaType.APPLICATION_JSON));

        BiorxivPage page = client.fetchPage(DAY, DAY, 30);

        assertThat(page.total()).isEqualTo(308);
        assertThat(page.articles()).hasSize(2);
        BiorxivArticleDto first = page.articles().getFirst();
        assertThat(first.doi()).isEqualTo("10.1101/2026.11.10.000001");
        assertThat(first.title()).isEqualTo("A first preprint");
        assertThat(first.authors()).isEqualTo("Doe, J.; Roe, R.");
        assertThat(first.authorCorresponding()).isEqualTo("Jane Doe");
        assertThat(first.authorCorrespondingInstitution()).isEqualTo("Some University");
        assertThat(first.date()).isEqualTo("2026-11-12");
        assertThat(first.version()).isEqualTo("1");
        assertThat(first.type()).isEqualTo("new results");
        assertThat(first.license()).isEqualTo("cc_by");
        assertThat(first.category()).isEqualTo("neuroscience");
        assertThat(first.jatsxml()).endsWith(".source.xml");
        assertThat(first.abstractText()).isEqualTo("Abstract one.");
        assertThat(first.published()).isEqualTo("NA");
        assertThat(first.server()).isEqualTo("bioRxiv");
        assertThat(page.articles().get(1).published()).isEqualTo("10.1038/s41586-026-00001-1");
        server.verify();
    }

    @Test
    void noPostsFoundIsAnEmptyPage() {
        server.expect(requestTo("https://api.biorxiv.org/details/biorxiv/2026-11-12/2026-11-13/0/json"))
                .andRespond(withSuccess(new ClassPathResource("biorxiv/details-empty.json"), MediaType.APPLICATION_JSON));

        BiorxivPage page = client.fetchPage(DAY, DAY.plusDays(1), 0);

        assertThat(page.articles()).isEmpty();
        assertThat(page.total()).isZero();
    }

    @Test
    void serverErrorPropagates() {
        server.expect(requestTo("https://api.biorxiv.org/details/biorxiv/2026-11-12/2026-11-12/0/json"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.fetchPage(DAY, DAY, 0)).isInstanceOf(HttpServerErrorException.class);
    }
}
