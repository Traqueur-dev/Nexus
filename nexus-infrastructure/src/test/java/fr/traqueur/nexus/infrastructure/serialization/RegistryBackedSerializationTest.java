package fr.traqueur.nexus.infrastructure.serialization;

import fr.traqueur.nexus.application.registry.Registries;
import fr.traqueur.nexus.application.registry.Registry;
import fr.traqueur.nexus.domain.events.Context;
import fr.traqueur.nexus.domain.events.ContextMetadata;
import fr.traqueur.nexus.infrastructure.TestJson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The mechanism itself, on whichever hierarchy is convenient — {@code Context} —
 * rather than repeated once per hierarchy. What each hierarchy owes on its own is
 * its wire format, which its own test asserts.
 */
@DisplayName("Registry-backed serialization")
class RegistryBackedSerializationTest {

    /**
     * A registered type that is not a record. Serialization walks record
     * components, so this is the shape that cannot be written — and the previous
     * mechanism wrote it happily but lost every field.
     */
    @ContextMetadata(type = "legacy")
    static class LegacyContext implements Context {

        public String getServer() {
            return "survival";
        }
    }

    @Test
    @DisplayName("should refuse to serialize a registered type that is not a record")
    void shouldRefuseANonRecordType() {
        Registry<Context, ContextMetadata> contexts = Registries.contexts().register(LegacyContext.class);
        ObjectMapper json = TestJson.mapper(contexts);

        // Loud rather than lossy: writing only {"source":"legacy"} would store a
        // context stripped of its data and fail much later, on the read.
        assertThatThrownBy(() -> json.writeValueAsString(new LegacyContext()))
                .hasMessageContaining("must be a record");
    }

    @Test
    @DisplayName("should reject a payload that is not a JSON object")
    void shouldRejectANonObjectPayload() {
        ObjectMapper json = TestJson.mapper();

        assertThatThrownBy(() -> json.readValue("\"discord\"", Context.class))
                .hasMessageContaining("must be a JSON object");
    }
}