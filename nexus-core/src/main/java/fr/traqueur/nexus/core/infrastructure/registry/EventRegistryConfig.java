package fr.traqueur.nexus.core.infrastructure.registry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventRegistryConfig {

    @Bean
    public EventRegistry eventRegistry() {
        return new EventRegistry();
    }

}
