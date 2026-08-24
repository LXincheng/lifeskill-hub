package dev.lifeskill.shared.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
