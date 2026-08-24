package fr.traqueur.nexus.domain.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EventType")
class EventTypeTest {

    @Test
    @DisplayName("should accept a conventional identifier")
    void shouldAcceptConventionalIdentifier() {
        assertThat(EventType.of("github.push_received").value()).isEqualTo("github.push_received");
    }

    @Test
    @DisplayName("should not impose the source.name convention on adapters")
    void shouldNotImposeConvention() {
        assertThat(EventType.of("minecraft:player_joined")).isNotNull();
        assertThat(EventType.of("custom")).isNotNull();
    }

    @Test
    @DisplayName("should reject null")
    void shouldRejectNull() {
        assertThatThrownBy(() -> EventType.of(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("should reject blank")
    void shouldRejectBlank() {
        assertThatThrownBy(() -> EventType.of("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("should reject whitespace so it stays usable as a key")
    void shouldRejectWhitespace() {
        assertThatThrownBy(() -> EventType.of("github push"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("whitespace");
    }

    @Test
    @DisplayName("should compare by value")
    void shouldCompareByValue() {
        assertThat(EventType.of("a.b")).isEqualTo(EventType.of("a.b"));
        assertThat(EventType.of("a.b")).isNotEqualTo(EventType.of("a.c"));
    }
}
