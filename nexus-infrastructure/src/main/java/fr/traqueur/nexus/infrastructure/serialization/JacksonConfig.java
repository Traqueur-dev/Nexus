package fr.traqueur.nexus.infrastructure.serialization;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.traqueur.nexus.application.registry.Registry;
import fr.traqueur.nexus.domain.events.Context;
import fr.traqueur.nexus.domain.events.ContextMetadata;
import fr.traqueur.nexus.domain.workflow.Condition;
import fr.traqueur.nexus.domain.workflow.ConditionMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    private final Registry<Condition, ConditionMetadata> conditions;
    private final Registry<Context, ContextMetadata> contexts;

    public JacksonConfig(Registry<Condition, ConditionMetadata> conditions,
                         Registry<Context, ContextMetadata> contexts) {
        this.conditions = conditions;
        this.contexts = contexts;
    }

    @Bean
    public ObjectMapper objectMapper() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Condition.class, new ConditionSerialization.Deserializer(conditions));
        module.addSerializer(Condition.class, new ConditionSerialization.Serializer(conditions));

        ObjectMapper mapper = JsonMapper.builder()
                .configure(MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_TIMES, false)
                .addMixIn(Context.class, ContextMixin.class)
                .addModule(new JavaTimeModule())
                .addModule(module)
                .build();

        registerContextSubtypes(mapper);
        return mapper;
    }

    /**
     * Teaches Jackson every context type the registry knows about.
     *
     * <p>Contexts are resolved from the registry rather than from a hardcoded
     * {@code @JsonSubTypes} list, so an adapter's context type is deserializable
     * as soon as it is registered.
     *
     * <p>Note the current limit: this runs once, when the mapper is built. A
     * plugin loaded after startup will need its subtypes registered on the live
     * mapper too — see the plugin loader work in Phase 2.
     */
    private void registerContextSubtypes(ObjectMapper mapper) {
        for (Class<? extends Context> type : contexts.registeredClasses()) {
            mapper.registerSubtypes(new NamedType(type, contexts.requireTypeForClass(type)));
        }
    }

}
