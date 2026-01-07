package fr.traqueur.nexus.core.infrastructure.registry;

import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.EventMetadata;
import fr.traqueur.nexus.core.domain.workflow.Condition;
import fr.traqueur.nexus.core.domain.workflow.ConditionMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RegistriesConfig {

    @Bean
    public Registry<Event, EventMetadata> eventRegistry() {
        return new Registry<>(Event.class, EventMetadata.class, EventMetadata::type);
    }

    @Bean
    public Registry<Condition, ConditionMetadata> conditionRegistry() { return new Registry<>(Condition.class, ConditionMetadata.class, ConditionMetadata::type); }

}
