package fr.traqueur.nexus.infrastructure.serialization;

import fr.traqueur.nexus.application.registry.Registries;
import fr.traqueur.nexus.application.registry.Registry;
import fr.traqueur.nexus.domain.events.Context;
import fr.traqueur.nexus.domain.events.ContextMetadata;
import fr.traqueur.nexus.domain.events.discord.DiscordContext;
import fr.traqueur.nexus.domain.events.github.GitHubContext;
import fr.traqueur.nexus.infrastructure.TestJson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers polymorphic context serialization, now resolved through the registry at
 * call time rather than through a subtype list frozen when the mapper was built.
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
    @DisplayName("should deserialize a context registered after the mapper was built")
    void shouldResolveATypeRegisteredAfterTheMapperWasBuilt() throws Exception {
        // The reason this mechanism replaced the mixin. Registering the subtypes
        // from the registry still read them once, when the mapper was built — so a
        // plugin loading at T+5min was in the registry and undeserializable, and
        // the fix would have been to rebuild the mapper every bean already holds.
        Registry<Context, ContextMetadata> contexts = Registries.contexts();
        ObjectMapper mapper = TestJson.mapper(contexts);

        contexts.register(MinecraftContext.class);

        Context decoded = mapper.readValue("{\"source\":\"minecraft\",\"server\":\"survival\"}", Context.class);
        assertThat(decoded).isEqualTo(new MinecraftContext("survival"));
        assertThat(mapper.writeValueAsString(decoded)).contains("\"source\":\"minecraft\"");
    }

    @Test
    @DisplayName("should fail on a context type that was never registered")
    void shouldFailOnUnregisteredContext() {
        assertThatThrownBy(() -> json.readValue("{\"source\":\"minecraft\"}", Context.class))
                .hasMessageContaining("Unknown context type: minecraft");
    }

    @Test
    @DisplayName("should fail on a context payload carrying no discriminator")
    void shouldFailOnMissingDiscriminator() {
        assertThatThrownBy(() -> json.readValue("{\"server\":\"survival\"}", Context.class))
                .hasMessageContaining("Context is missing its 'source' field");
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