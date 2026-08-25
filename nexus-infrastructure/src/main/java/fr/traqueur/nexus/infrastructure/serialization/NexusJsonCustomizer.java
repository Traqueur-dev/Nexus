package fr.traqueur.nexus.infrastructure.serialization;

import fr.traqueur.nexus.application.registry.Registry;
import fr.traqueur.nexus.domain.events.Context;
import fr.traqueur.nexus.domain.events.ContextMetadata;
import fr.traqueur.nexus.domain.workflow.Action;
import fr.traqueur.nexus.domain.workflow.ActionMetadata;
import fr.traqueur.nexus.domain.workflow.Condition;
import fr.traqueur.nexus.domain.workflow.ConditionMetadata;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * Everything Nexus adds to the JSON mapper: one registry-backed hierarchy per line.
 *
 * <p>This customizes the mapper Spring Boot builds rather than declaring a second
 * one. Declaring a {@code @Bean ObjectMapper} does not replace the auto-configured
 * mapper: {@code JacksonAutoConfiguration} backs off on a missing bean of type
 * {@code JsonMapper}, which a bean declared as {@code ObjectMapper} does not
 * match, and the mapper it then builds is {@code @Primary}. The result is two
 * mappers where injection picks Boot's — the configured one is silently unused,
 * which is exactly how contexts stored to PostgreSQL lost their {@code source}
 * discriminator and could not be read back.
 *
 * <p>A customizer keeps a single mapper in the application, so persistence,
 * messaging and HTTP responses all encode a context the same way.
 */
public class NexusJsonCustomizer implements JsonMapperBuilderCustomizer {

    /**
     * The discriminator each hierarchy is written under. Both are persisted
     * contracts — {@code source} names a context in the {@code events.context}
     * column and in every queued message, {@code type} names a condition or an
     * action inside a stored workflow. Renaming one breaks the rows already
     * written.
     */
    private static final String SOURCE = "source";
    private static final String TYPE = "type";

    private final Registry<Condition, ConditionMetadata> conditions;
    private final Registry<Context, ContextMetadata> contexts;
    private final Registry<Action, ActionMetadata> actions;

    public NexusJsonCustomizer(Registry<Condition, ConditionMetadata> conditions,
                               Registry<Context, ContextMetadata> contexts,
                               Registry<Action, ActionMetadata> actions) {
        this.conditions = conditions;
        this.contexts = contexts;
        this.actions = actions;
    }

    @Override
    public void customize(JsonMapper.Builder builder) {
        SimpleModule module = new SimpleModule();
        RegistryBackedSerialization.register(module, Context.class, contexts, SOURCE);
        RegistryBackedSerialization.register(module, Condition.class, conditions, TYPE);
        RegistryBackedSerialization.register(module, Action.class, actions, TYPE);

        builder.addModule(module);
    }
}