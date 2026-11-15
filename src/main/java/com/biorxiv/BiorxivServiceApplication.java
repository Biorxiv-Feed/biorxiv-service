package com.biorxiv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BiorxivServiceApplication {

	public static void main(String[] args) {
		// All timestamps are UTC. Hibernate's @CreationTimestamp for LocalDateTime uses the JVM default zone.
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(BiorxivServiceApplication.class, args);
	}

}
