package fr.traqueur.nexus.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.traqueur.nexus.core.application.registry.Registry;
import fr.traqueur.nexus.core.bootstrap.RegistriesConfig;
import fr.traqueur.nexus.core.domain.events.Context;
import fr.traqueur.nexus.core.domain.events.ContextMetadata;
import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.EventMetadata;
import fr.traqueur.nexus.core.domain.workflow.Action;
import fr.traqueur.nexus.core.domain.workflow.ActionMetadata;
import fr.traqueur.nexus.core.domain.workflow.Condition;
import fr.traqueur.nexus.core.domain.workflow.ConditionMetadata;
import fr.traqueur.nexus.core.infrastructure.serialization.JacksonConfig;

/**
 * Shared test fixtures.
 *
 * <p>Two reasons this exists rather than each test building its own:
 *
 * <ul>
 *   <li>The {@code new Registry<>(X.class, XMetadata.class, XMetadata::type)} triple
 *       is fixed per hierarchy. Repeating it per test means every new registered
 *       hierarchy lands in a dozen places.
 *   <li>More importantly, {@link #objectMapper()} delegates to the real
 *       {@link JacksonConfig}. Tests that hand-rolled an equivalent mapper had
 *       already drifted from it — {@code findAndRegisterModules()} instead of the
 *       configured time handling, and no condition serializer — so they were
 *       asserting round-trips through a mapper production never uses.
 * </ul>
 */
public final class TestFixtures {

    private TestFixtures() {
    }

    private static final RegistriesConfig REGISTRIES = new RegistriesConfig();

    public static Registry<Event, EventMetadata> events() {
        return REGISTRIES.eventRegistry();
    }

    public static Registry<Context, ContextMetadata> contexts() {
        return REGISTRIES.contextRegistry();
    }

    public static Registry<Condition, ConditionMetadata> conditions() {
        return REGISTRIES.conditionRegistry();
    }

    public static Registry<Action, ActionMetadata> actions() {
        return REGISTRIES.actionRegistry();
    }

    /** The production mapper, configured exactly as the application configures it. */
    public static ObjectMapper objectMapper() {
        return objectMapper(contexts());
    }

    /** The production mapper, with a context registry a test can extend. */
    public static ObjectMapper objectMapper(Registry<Context, ContextMetadata> contexts) {
        return new JacksonConfig(conditions(), contexts).objectMapper();
    }
}
