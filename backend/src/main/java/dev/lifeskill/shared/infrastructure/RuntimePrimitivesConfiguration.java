package dev.lifeskill.shared.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.Executor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import dev.lifeskill.shared.application.IdGenerator;

@Configuration
public class RuntimePrimitivesConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    IdGenerator randomIdGenerator() {
        return java.util.UUID::randomUUID;
    }

    @Bean("agentRunExecutor")
    Executor agentRunExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("agent-run-");
        executor.initialize();
        return executor;
    }
}
