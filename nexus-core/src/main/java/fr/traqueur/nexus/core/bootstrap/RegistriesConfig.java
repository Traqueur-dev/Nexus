package fr.traqueur.nexus.core.bootstrap;

import fr.traqueur.nexus.core.application.registry.Registry;
import fr.traqueur.nexus.core.domain.events.Context;
import fr.traqueur.nexus.core.domain.events.ContextMetadata;
import fr.traqueur.nexus.core.domain.events.CoreContexts;
import fr.traqueur.nexus.core.domain.events.CoreEvents;
import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.EventMetadata;
import fr.traqueur.nexus.core.domain.workflow.Condition;
import fr.traqueur.nexus.core.domain.workflow.ConditionMetadata;
import fr.traqueur.nexus.core.domain.workflow.CoreConditions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RegistriesConfig {

    @Bean
    public Registry<Event, EventMetadata> eventRegistry() {
        return new Registry<>(Event.class, EventMetadata.class, EventMetadata::type)
                .registerAll(CoreEvents.types());
    }

    @Bean
    public Registry<Context, ContextMetadata> contextRegistry() {
        return new Registry<>(Context.class, ContextMetadata.class, ContextMetadata::type)
                .registerAll(CoreContexts.types());
    }

    @Bean
    public Registry<Condition, ConditionMetadata> conditionRegistry() {
        return new Registry<>(Condition.class, ConditionMetadata.class, ConditionMetadata::type)
                .registerAll(CoreConditions.types());
    }

}
