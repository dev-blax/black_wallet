package com.james.wallet;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Observability wiring (Phase 8).
 *
 * Micrometer ships the @Observed and @Timed annotations, but the aspects that intercept them
 * are NOT registered automatically — you must declare them as beans. With these in place:
 *
 *  - @Observed(name = "x")  → creates an "x" timer metric AND a tracing span around the method.
 *  - @Timed(value = "y")    → creates a "y" timer metric around the method.
 *
 * One @Observed annotation therefore feeds BOTH the metrics pillar (Prometheus) and the tracing
 * pillar (trace/span ids) from a single point in the code.
 */
@Configuration
public class ObservabilityConfig {

    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry meterRegistry) {
        return new TimedAspect(meterRegistry);
    }
}
