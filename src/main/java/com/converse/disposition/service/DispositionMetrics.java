package com.converse.disposition.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class DispositionMetrics {

    private final MeterRegistry registry;
    private final Counter cacheHits;
    private final Counter cacheMisses;

    public DispositionMetrics(MeterRegistry registry) {
        this.registry   = registry;
        this.cacheHits  = Counter.builder("disposition.cache.hits").register(registry);
        this.cacheMisses = Counter.builder("disposition.cache.misses").register(registry);
    }

    public void recordLatency(Duration d, String outcome) {
        Timer.builder("disposition.latency")
                .tag("outcome", outcome)
                .register(registry)
                .record(d);
    }

    public void recordBedrockCall(String outcome) {
        Counter.builder("disposition.bedrock.calls")
                .tag("outcome", outcome)
                .register(registry)
                .increment();
    }

    public void recordBedrockTokens(String type, long count) {
        DistributionSummary.builder("disposition.bedrock.tokens")
                .tag("type", type)
                .register(registry)
                .record(count);
    }

    public void recordDeliveryFailure(String target) {
        Counter.builder("disposition.delivery.failures")
                .tag("target", target)
                .register(registry)
                .increment();
    }

    public void recordCacheHit()  { cacheHits.increment(); }
    public void recordCacheMiss() { cacheMisses.increment(); }
}
