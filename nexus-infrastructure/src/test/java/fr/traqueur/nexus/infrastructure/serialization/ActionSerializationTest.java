package fr.traqueur.nexus.infrastructure.serialization;

import fr.traqueur.nexus.domain.workflow.Action;
import fr.traqueur.nexus.domain.workflow.actions.SendEmailAction;
import fr.traqueur.nexus.infrastructure.TestJson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Actions are the third hierarchy on the shared mechanism, and the reason it was
 * generalised now rather than during the plugin loader work: persisting a workflow
 * (#6) means writing its actions, and they gained their identity in #30. Nothing
 * serializes an action in production yet — these tests are what says the format is
 * decided before a row depends on it.
 */
@DisplayName("Action serialization")
class ActionSerializationTest {

    private ObjectMapper json;

    @BeforeEach
    void setUp() {
        json = TestJson.mapper();
    }

    @Test
    @DisplayName("should write an action under its registered identifier")
    void shouldSerializeAction() throws Exception {
        String encoded = json.writeValueAsString(new SendEmailAction("Subject", "Body", "dev@example.com"));

        assertThat(encoded)
                .contains("\"type\":\"send_email\"")
                .contains("\"subject\":\"Subject\"")
                .contains("\"to\":\"dev@example.com\"");
    }

    @Test
    @DisplayName("should round-trip an action read as the base type")
    void shouldRoundTripAction() throws Exception {
        Action original = new SendEmailAction("Subject", "Body", "dev@example.com");

        Action restored = json.readValue(json.writeValueAsString(original), Action.class);

        assertThat(restored).isEqualTo(original);
    }

    @Test
    @DisplayName("should fail on an action type that was never registered")
    void shouldFailOnUnregisteredAction() {
        assertThatThrownBy(() -> json.readValue("{\"type\":\"send_carrier_pigeon\"}", Action.class))
                .hasMessageContaining("Unknown action type: send_carrier_pigeon");
    }
}