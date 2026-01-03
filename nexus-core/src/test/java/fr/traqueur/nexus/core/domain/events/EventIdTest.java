package fr.traqueur.nexus.core.domain.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class EventIdTest {

    @Nested
    @DisplayName("fromString")
    class FromString {

        @Test
        @DisplayName("should parse valid id")
        void shouldParseValidId() {
            Event.Id id = Event.Id.fromString("discord-abc123");

            assertThat(id.prefix()).isEqualTo("discord");
            assertThat(id.instance()).isEqualTo("abc123");
        }

        @Test
        @DisplayName("should parse id with long prefix")
        void shouldParseIdWithLongPrefix() {
            Event.Id id = Event.Id.fromString("minecraft-xyz789");

            assertThat(id.prefix()).isEqualTo("minecraft");
            assertThat(id.instance()).isEqualTo("xyz789");
        }

        @Test
        @DisplayName("should throw on invalid format - no separator")
        void shouldThrowOnNoSeparator() {
            assertThatThrownBy(() -> Event.Id.fromString("discordabc123"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw on invalid format - empty string")
        void shouldThrowOnEmptyString() {
            assertThatThrownBy(() -> Event.Id.fromString(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("generate")
    class Generate {

        @Test
        @DisplayName("should generate id with correct prefix")
        void shouldGenerateIdWithCorrectPrefix() {
            Event.Id id = Event.Id.generate("discord");

            assertThat(id.prefix()).isEqualTo("discord");
            assertThat(id.instance()).hasSize(6);
            assertThat(id.instance()).matches("[a-z0-9]{6}");
        }

        @Test
        @DisplayName("should generate unique instances")
        void shouldGenerateUniqueInstances() {
            Event.Id id1 = Event.Id.generate("test");
            Event.Id id2 = Event.Id.generate("test");

            assertThat(id1.instance()).isNotEqualTo(id2.instance());
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("should format as prefix-instance")
        void shouldFormatCorrectly() {
            Event.Id id = new Event.Id("github", "abc123");

            assertThat(id.toString()).isEqualTo("github-abc123");
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("should reject null prefix")
        void shouldRejectNullPrefix() {
            assertThatThrownBy(() -> new Event.Id(null, "abc123"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should reject null instance")
        void shouldRejectNullInstance() {
            assertThatThrownBy(() -> new Event.Id("test", null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should reject uppercase prefix")
        void shouldRejectUppercasePrefix() {
            assertThatThrownBy(() -> new Event.Id("Discord", "abc123"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject prefix with numbers")
        void shouldRejectPrefixWithNumbers() {
            assertThatThrownBy(() -> new Event.Id("test123", "abc123"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject instance with wrong length")
        void shouldRejectInstanceWithWrongLength() {
            assertThatThrownBy(() -> new Event.Id("test", "abc12"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject instance with uppercase")
        void shouldRejectInstanceWithUppercase() {
            assertThatThrownBy(() -> new Event.Id("test", "ABC123"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}