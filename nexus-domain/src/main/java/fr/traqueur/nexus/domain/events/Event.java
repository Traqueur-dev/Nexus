package fr.traqueur.nexus.domain.events;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;

/**
 * An event ingested by Nexus.
 *
 * <p>This hierarchy is intentionally open: adapters — bundled or third-party —
 * contribute their own event types and register them in the event registry.
 * Implementations are expected to be immutable records annotated with
 * {@link EventMetadata}.
 */
public interface Event {

    record Id(String prefix, String instance) {

        private static final SecureRandom RANDOM = new SecureRandom();
        private static final String CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

        public Id {
            Objects.requireNonNull(prefix);
            if(!prefix.matches("[a-z]+")) {
                throw new IllegalArgumentException("prefix must be lowercase characters");
            }

            Objects.requireNonNull(instance);
            if(!instance.matches("[a-z0-9]{6}")) {
                throw new IllegalArgumentException("instance must be exactly 6 lowercase alphanumeric characters");
            }
        }

        public static Id fromString(String id) {
            String[] parts = id.split("-");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid id format");
            }
            return new Id(parts[0], parts[1]);
        }

        @Override
        public String toString() {
            return String.format("%s-%s", prefix, instance);
        }

        public static Id generate(String prefix) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                int index = RANDOM.nextInt(CHARS.length());
                char character = CHARS.charAt(index);
                builder.append(character);
            }
            return new Id(prefix, builder.toString());
        }

    }

    Id id();

    Context context();

    Instant timestamp();

}
