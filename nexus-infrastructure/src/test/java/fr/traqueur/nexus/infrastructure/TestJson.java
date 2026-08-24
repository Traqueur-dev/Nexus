package fr.traqueur.nexus.infrastructure;

import fr.traqueur.nexus.application.registry.Registries;
import fr.traqueur.nexus.application.registry.Registry;
import fr.traqueur.nexus.domain.events.Context;
import fr.traqueur.nexus.domain.events.ContextMetadata;
import fr.traqueur.nexus.domain.workflow.Condition;
import fr.traqueur.nexus.domain.workflow.ConditionMetadata;
import fr.traqueur.nexus.infrastructure.serialization.NexusJsonCustomizer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * A mapper carrying the production serialization, for tests that need no Spring.
 *
 * <p>Built by running the production customizer over a plain builder. Hand-rolled
 * equivalents drift: earlier versions used {@code findAndRegisterModules()} and
 * registered no condition serializer, so they asserted round-trips through a
 * mapper the application never builds.
 *
 * <p>What this cannot cover is the other half of the same question — whether the
 * customizer actually reaches the mapper the application injects. Only a Spring
 * context answers that; see {@code JacksonConfigTest} in nexus-bootstrap.
 */
public final class TestJson {

    private TestJson() {
    }

    public static ObjectMapper mapper() {
        return mapper(Registries.contexts());
    }

    public static ObjectMapper mapper(Registry<Context, ContextMetadata> contexts) {
        return mapper(Registries.conditions(), contexts);
    }

    public static ObjectMapper mapper(Registry<Condition, ConditionMetadata> conditions,
                                      Registry<Context, ContextMetadata> contexts) {
        JsonMapper.Builder builder = JsonMapper.builder();
        new NexusJsonCustomizer(conditions, contexts).customize(builder);
        return builder.build();
    }
}