package com.biorxiv;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
// The scheduler must never call the real API from tests.
@SpringBootTest(properties = "biorxiv.fetch.enabled=false")
class BiorxivServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
