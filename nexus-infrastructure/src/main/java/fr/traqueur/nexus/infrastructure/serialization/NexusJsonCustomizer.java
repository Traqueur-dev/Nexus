package fr.traqueur.nexus.infrastructure.serialization;

import fr.traqueur.nexus.application.registry.Registry;
import fr.traqueur.nexus.domain.events.Context;
import fr.traqueur.nexus.domain.events.ContextMetadata;
import fr.traqueur.nexus.domain.workflow.Condition;
import fr.traqueur.nexus.domain.workflow.ConditionMetadata;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.module.SimpleModule;

import java.util.List;

/**
 * Everything Nexus adds to the JSON mapper: polymorphic contexts and conditions.
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

    private final Registry<Condition, ConditionMetadata> conditions;
    private final Registry<Context, ContextMetadata> contexts;

    public NexusJsonCustomizer(Registry<Condition, ConditionMetadata> conditions,
                               Registry<Context, ContextMetadata> contexts) {
        this.conditions = conditions;
        this.contexts = contexts;
    }

    @Override
    public void customize(JsonMapper.Builder builder) {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Condition.class, new ConditionSerialization.Deserializer(conditions));
        module.addSerializer(Condition.class, new ConditionSerialization.Serializer(conditions));

        builder.addMixIn(Context.class, ContextMixin.class)
                .registerSubtypes(contextSubtypes())
                .addModule(module);
    }

    /**
     * Every context type the registry knows about.
     *
     * <p>Resolved from the registry rather than a hardcoded {@code @JsonSubTypes}
     * list, so an adapter's context type is deserializable once registered.
     *
     * <p>Current limit: this is read when the mapper is built. A plugin loaded
     * after startup needs its subtypes registered on the live mapper too — see
     * the plugin loader work in Phase 2.
     */
    private NamedType[] contextSubtypes() {
        List<NamedType> subtypes = contexts.registeredClasses().stream()
                .map(type -> new NamedType(type, contexts.requireTypeForClass(type)))
                .toList();
        return subtypes.toArray(NamedType[]::new);
    }
}