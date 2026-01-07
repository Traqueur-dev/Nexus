package fr.traqueur.nexus.core.infrastructure.serialization;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.traqueur.nexus.core.application.registry.Registry;
import fr.traqueur.nexus.core.domain.events.Context;
import fr.traqueur.nexus.core.domain.workflow.Condition;
import fr.traqueur.nexus.core.domain.workflow.ConditionMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    private final Registry<Condition, ConditionMetadata> registry;

    public JacksonConfig(Registry<Condition, ConditionMetadata> registry) {
        this.registry = registry;
    }

    @Bean
    public ObjectMapper objectMapper() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Condition.class, new ConditionSerialization.Deserializer(registry));
        module.addSerializer(Condition.class, new ConditionSerialization.Serializer(registry));

        return JsonMapper.builder()
                .configure(MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_TIMES, false)
                .addMixIn(Context.class, ContextMixin.class)
                .addModule(new JavaTimeModule())
                .addModule(module)
                .build();
    }

}
