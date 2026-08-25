package fr.traqueur.nexus.infrastructure.serialization;

import fr.traqueur.nexus.application.registry.Registry;
import fr.traqueur.nexus.domain.events.Context;
import fr.traqueur.nexus.domain.events.ContextMetadata;
import fr.traqueur.nexus.domain.workflow.Action;
import fr.traqueur.nexus.domain.workflow.ActionMetadata;
import fr.traqueur.nexus.domain.workflow.Condition;
import fr.traqueur.nexus.domain.workflow.ConditionMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * Contributes Nexus' serialization to the auto-configured mapper.
     *
     * <p>No {@code ObjectMapper} bean is declared here on purpose — see
     * {@link NexusJsonCustomizer} for what declaring one silently does.
     */
    @Bean
    public NexusJsonCustomizer nexusJsonCustomizer(Registry<Condition, ConditionMetadata> conditions,
                                                   Registry<Context, ContextMetadata> contexts,
                                                   Registry<Action, ActionMetadata> actions) {
        return new NexusJsonCustomizer(conditions, contexts, actions);
    }
}