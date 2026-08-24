package fr.traqueur.nexus.infrastructure.serialization;

import fr.traqueur.nexus.application.registry.Registry;
import fr.traqueur.nexus.domain.events.Context;
import fr.traqueur.nexus.domain.events.ContextMetadata;
import fr.traqueur.nexus.domain.workflow.Condition;
import fr.traqueur.nexus.domain.workflow.ConditionMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.module.SimpleModule;

import java.util.List;

@Configuration
public class JacksonConfig {

    private final Registry<Condition, ConditionMetadata> conditions;
    private final Registry<Context, ContextMetadata> contexts;

    public JacksonConfig(Registry<Condition, ConditionMetadata> conditions,
                         Registry<Context, ContextMetadata> contexts) {
        this.conditions = conditions;
        this.contexts = contexts;
    }

    /**
     * The application's mapper.
     *
     * <p>Jackson 3, matching what Spring Boot 4 wires into its message converters:
     * a Jackson 2 mapper here would leave the HTTP layer serializing through a
     * different, unconfigured object. java.time support is built in, so no module
     * is registered for it.
     */
    @Bean
    public ObjectMapper objectMapper() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Condition.class, new ConditionSerialization.Deserializer(conditions));
        module.addSerializer(Condition.class, new ConditionSerialization.Serializer(conditions));

        return JsonMapper.builder()
                .addMixIn(Context.class, ContextMixin.class)
                .registerSubtypes(contextSubtypes())
                .addModule(module)
                .build();
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
        return subtypes.toArray(new NamedType[0]);
    }

}
