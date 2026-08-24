package fr.traqueur.nexus.infrastructure.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.traqueur.nexus.infrastructure.TestJson;
import fr.traqueur.nexus.application.registry.Registries;
import fr.traqueur.nexus.domain.events.Context;
import fr.traqueur.nexus.domain.events.ContextMetadata;
import fr.traqueur.nexus.domain.events.discord.DiscordContext;
import fr.traqueur.nexus.domain.events.github.GitHubContext;
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

    private ObjectMapper json;

    @BeforeEach
    void setUp() {
        json = TestJson.mapper();
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
        ObjectMapper mapper = TestJson.mapper(
                Registries.contexts().register(MinecraftContext.class));

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
