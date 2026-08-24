package fr.traqueur.nexus.infrastructure;

import tools.jackson.databind.ObjectMapper;
import fr.traqueur.nexus.application.registry.Registries;
import fr.traqueur.nexus.application.registry.Registry;
import fr.traqueur.nexus.domain.events.Context;
import fr.traqueur.nexus.domain.events.ContextMetadata;
import fr.traqueur.nexus.infrastructure.serialization.JacksonConfig;

/**
 * The production mapper for tests.
 *
 * <p>Hand-rolled equivalents drift: earlier versions used
 * {@code findAndRegisterModules()} instead of the configured time handling and
 * registered no condition serializer, so they asserted round-trips through a
 * mapper the application never builds.
 */
public final class TestJson {

    private TestJson() {
    }

    public static ObjectMapper mapper() {
        return mapper(Registries.contexts());
    }

    public static ObjectMapper mapper(Registry<Context, ContextMetadata> contexts) {
        return new JacksonConfig(Registries.conditions(), contexts).objectMapper();
    }
}
