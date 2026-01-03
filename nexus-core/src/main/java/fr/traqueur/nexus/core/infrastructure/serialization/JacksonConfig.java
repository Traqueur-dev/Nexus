package fr.traqueur.nexus.core.infrastructure.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.traqueur.nexus.core.domain.events.Context;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.addMixIn(Context.class, ContextMixin.class);
        return mapper;
    }

}
