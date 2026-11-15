package com.biorxiv.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables {@code @Scheduled} jobs and Spring Framework's {@code @Retryable} (used by the bioRxiv client).
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableResilientMethods
public class SchedulingConfig {
}
