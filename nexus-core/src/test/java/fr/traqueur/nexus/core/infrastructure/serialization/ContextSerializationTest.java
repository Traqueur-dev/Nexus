package fr.traqueur.nexus.core.infrastructure.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import fr.traqueur.nexus.core.application.registry.Registry;
import fr.traqueur.nexus.core.domain.events.Context;
import fr.traqueur.nexus.core.domain.events.ContextMetadata;
import fr.traqueur.nexus.core.domain.events.CoreContexts;
import fr.traqueur.nexus.core.domain.events.discord.DiscordContext;
import fr.traqueur.nexus.core.domain.events.github.GitHubContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers polymorphic context serialization once the subtype list stopped being
 * hardcoded in {@code ContextMixin}.
 */
@DisplayName("Context serialization")
class ContextSerializationTest {

    /** A context type declared outside the domain, standing in for a plugin's. */
    @ContextMetadata(type = "minecraft")
    record MinecraftContext(String server) implements Context {
    }

    private Registry<Context, ContextMetadata> registry;
    private ObjectMapper json;

    @BeforeEach
    void setUp() {
        registry = new Registry<>(Context.class, ContextMetadata.class, ContextMetadata::type)
                .registerAll(CoreContexts.types());
        json = buildMapper(registry);
    }

    private static ObjectMapper buildMapper(Registry<Context, ContextMetadata> registry) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.addMixIn(Context.class, ContextMixin.class);
        mapper.findAndRegisterModules();
        for (Class<? extends Context> type : registry.registeredClasses()) {
            mapper.registerSubtypes(new NamedType(type, registry.requireTypeForClass(type)));
        }
        return mapper;
    }

    @Test
    @DisplayName("should keep the existing wire format for built-in contexts")
    void shouldKeepWireFormat() throws Exception {
        assertThat(json.writeValueAsString(new DiscordContext())).isEqualTo("{\"source\":\"discord\"}");
        assertThat(json.readValue("{\"source\":\"github\"}", Context.class))
                .isInstanceOf(GitHubContext.class);
    }

    @Test
    @DisplayName("should derive source() from the metadata annotation")
    void shouldDeriveSourceFromAnnotation() {
        assertThat(new DiscordContext().source()).isEqualTo("discord");
        assertThat(new MinecraftContext("survival").source()).isEqualTo("minecraft");
    }

    @Test
    @DisplayName("should round-trip a context declared outside the domain")
    void shouldRoundTripExternalContext() throws Exception {
        // What the hardcoded @JsonSubTypes list made impossible: a context type
        // contributed by an adapter, deserialized without touching the core.
        ObjectMapper mapper = buildMapper(
                new Registry<Context, ContextMetadata>(Context.class, ContextMetadata.class, ContextMetadata::type)
                        .registerAll(CoreContexts.types())
                        .register(MinecraftContext.class));

        String encoded = mapper.writeValueAsString(new MinecraftContext("survival"));
        assertThat(encoded).contains("\"source\":\"minecraft\"").contains("\"server\":\"survival\"");

        Context decoded = mapper.readValue(encoded, Context.class);
        assertThat(decoded).isEqualTo(new MinecraftContext("survival"));
        assertThat(decoded.source()).isEqualTo("minecraft");
    }

    @Test
    @DisplayName("should fail on a context type that was never registered")
    void shouldFailOnUnregisteredContext() {
        assertThatThrownBy(() -> json.readValue("{\"source\":\"minecraft\"}", Context.class))
                .hasMessageContaining("minecraft");
    }

    @Test
    @DisplayName("should explain when a context carries no metadata annotation")
    void shouldExplainMissingAnnotation() {
        record Undeclared() implements Context {
        }

        assertThatThrownBy(() -> new Undeclared().source())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("@ContextMetadata");
    }
}
