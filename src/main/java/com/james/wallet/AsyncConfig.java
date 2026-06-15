package com.james.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration (Phase 7).
 *
 * @EnableAsync makes methods annotated with @Async run on a background thread pool instead of
 * the caller's thread. We define a dedicated, bounded executor so async side effects (e.g.
 * post-transfer notifications) can never exhaust threads or run unbounded.
 *
 * Implementing AsyncConfigurer lets us also supply an uncaught-exception handler: a @Async method
 * that returns void swallows exceptions by default (there is no Future for the caller to inspect),
 * so without this an async failure would vanish silently.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "eventTaskExecutor")
    public Executor eventTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("wallet-events-");
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return eventTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("Async method {} threw an exception", method.getName(), ex);
    }
}
