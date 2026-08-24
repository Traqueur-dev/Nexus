package fr.traqueur.nexus.domain.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventIdTest {

    private static final String UUID_A = "0192f3c4-8f2a-7c3d-9e1f-2a3b4c5d6e7f";
    private static final String UUID_B = "0192f3c4-9a1b-7d4e-8f2a-3b4c5d6e7f80";

    @Nested
    @DisplayName("fromString")
    class FromString {

        @Test
        @DisplayName("should parse valid id")
        void shouldParseValidId() {
            Event.Id id = Event.Id.fromString("discord-" + UUID_A);

            assertThat(id.prefix()).isEqualTo("discord");
            assertThat(id.instance()).isEqualTo(UUID_A);
        }

        @Test
        @DisplayName("should split on the first separator only")
        void shouldSplitOnFirstSeparatorOnly() {
            // The instance carries four hyphens of its own: splitting on every
            // separator would leave six fragments and lose the identifier.
            Event.Id id = Event.Id.fromString("minecraft-" + UUID_B);

            assertThat(id.prefix()).isEqualTo("minecraft");
            assertThat(id.instance()).isEqualTo(UUID_B);
        }

        @Test
        @DisplayName("should throw on invalid format - no separator")
        void shouldThrowOnNoSeparator() {
            assertThatThrownBy(() -> Event.Id.fromString("discord" + UUID_A))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw on invalid format - empty string")
        void shouldThrowOnEmptyString() {
            assertThatThrownBy(() -> Event.Id.fromString(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject an id in the pre-#27 format")
        void shouldRejectLegacyFormat() {
            // Six base-36 characters is exactly what made collisions possible.
            // Reading one back is a migration problem, not a format to keep
            // accepting quietly.
            assertThatThrownBy(() -> Event.Id.fromString("discord-abc123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("UUID");
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
            assertThat(id.instance()).matches(
                    "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }

        @Test
        @DisplayName("should generate a version 7 UUID")
        void shouldGenerateVersion7() {
            UUID generated = UUID.fromString(Event.Id.generate("discord").instance());

            assertThat(generated.version()).isEqualTo(7);
            assertThat(generated.variant()).isEqualTo(2);
        }

        @Test
        @DisplayName("should order ids by generation time")
        void shouldOrderByGenerationTime() throws InterruptedException {
            // What version 7 buys over version 4: an append-only store queried by
            // time can rely on the identifier's own order.
            Event.Id earlier = Event.Id.generate("test");
            Thread.sleep(2);
            Event.Id later = Event.Id.generate("test");

            assertThat(earlier.instance()).isLessThan(later.instance());
        }

        @Test
        @DisplayName("should generate unique instances")
        void shouldGenerateUniqueInstances() {
            // The point of #27: 10 000 ids for one source used to be well inside
            // the range where a collision was plausible.
            Set<String> instances = new HashSet<>();
            IntStream.range(0, 10_000)
                    .forEach(i -> instances.add(Event.Id.generate("test").instance()));

            assertThat(instances).hasSize(10_000);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("should format as prefix-instance")
        void shouldFormatCorrectly() {
            Event.Id id = new Event.Id("github", UUID_A);

            assertThat(id.toString()).isEqualTo("github-" + UUID_A);
        }

        @Test
        @DisplayName("should round-trip through fromString")
        void shouldRoundTrip() {
            Event.Id id = Event.Id.generate("github");

            assertThat(Event.Id.fromString(id.toString())).isEqualTo(id);
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("should reject null prefix")
        void shouldRejectNullPrefix() {
            assertThatThrownBy(() -> new Event.Id(null, UUID_A))
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
            assertThatThrownBy(() -> new Event.Id("Discord", UUID_A))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject prefix with numbers")
        void shouldRejectPrefixWithNumbers() {
            assertThatThrownBy(() -> new Event.Id("test123", UUID_A))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject a malformed instance")
        void shouldRejectMalformedInstance() {
            assertThatThrownBy(() -> new Event.Id("test", "not-a-uuid"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject an uppercase instance")
        void shouldRejectInstanceWithUppercase() {
            // One id, one spelling: an uppercase variant would be a different
            // primary key for the same event.
            assertThatThrownBy(() -> new Event.Id("test", UUID_A.toUpperCase()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}