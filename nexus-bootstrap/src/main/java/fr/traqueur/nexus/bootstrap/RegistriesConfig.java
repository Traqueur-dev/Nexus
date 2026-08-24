package fr.traqueur.nexus.bootstrap;

import fr.traqueur.nexus.application.registry.Registries;
import fr.traqueur.nexus.application.registry.Registry;
import fr.traqueur.nexus.domain.events.Context;
import fr.traqueur.nexus.domain.events.ContextMetadata;
import fr.traqueur.nexus.domain.events.Event;
import fr.traqueur.nexus.domain.events.EventMetadata;
import fr.traqueur.nexus.domain.workflow.Action;
import fr.traqueur.nexus.domain.workflow.ActionMetadata;
import fr.traqueur.nexus.domain.workflow.Condition;
import fr.traqueur.nexus.domain.workflow.ConditionMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the core registries as beans. The composition itself lives in
 * {@link Registries}, so it stays usable without Spring.
 */
@Configuration
public class RegistriesConfig {

    @Bean
    public Registry<Event, EventMetadata> eventRegistry() {
        return Registries.events();
    }

    @Bean
    public Registry<Context, ContextMetadata> contextRegistry() {
        return Registries.contexts();
    }

    @Bean
    public Registry<Condition, ConditionMetadata> conditionRegistry() {
        return Registries.conditions();
    }

    @Bean
    public Registry<Action, ActionMetadata> actionRegistry() {
        return Registries.actions();
    }
}
