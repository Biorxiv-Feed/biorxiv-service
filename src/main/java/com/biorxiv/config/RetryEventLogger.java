package com.biorxiv.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.resilience.retry.MethodRetryEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * Logs every failed attempt of a {@code @Retryable} method (Spring publishes a {@link MethodRetryEvent} each time).
 * Without this, retries are invisible: a call that succeeds on the third attempt looks like a slow call.
 */
@Slf4j
@Component
public class RetryEventLogger {

    @EventListener
    public void onRetry(MethodRetryEvent event) {
        String method = ClassUtils.getQualifiedMethodName(event.getMethod());
        if (event.isRetryAborted()) {
            // Aborted either because retries ran out or because the error is not retryable (e.g. 404).
            // The failure is Spring's RetryException; its cause is the last real error. The caller also
            // receives the exception and logs it with the stack trace, so no stack trace here.
            Throwable failure = event.getFailure();
            Throwable lastError = failure.getCause() != null ? failure.getCause() : failure;
            log.warn("Giving up on {}, no more retries: {}", method, lastError.toString());
        } else {
            // Published for every failed call; only transient errors (see @Retryable includes) are retried.
            log.warn("Call to {} failed: {}", method, event.getFailure().toString());
        }
    }
}
