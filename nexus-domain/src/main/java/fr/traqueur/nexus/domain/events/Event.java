package fr.traqueur.nexus.domain.events;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * An event ingested by Nexus.
 *
 * <p>This hierarchy is intentionally open: adapters — bundled or third-party —
 * contribute their own event types and register them in the event registry.
 * Implementations are expected to be immutable records annotated with
 * {@link EventMetadata}.
 */
public interface Event {

    /**
     * The identifier of an event: a source prefix and a UUID.
     *
     * <p>The prefix earns its place by making an id readable on its own — a log
     * line saying {@code github-0192f3c4-…} needs no join to be understood. The
     * instance is a UUID version 7, which is time-ordered: ids generated later
     * sort after ids generated earlier, which suits an append-only store that is
     * mostly queried by time.
     *
     * <p>It used to be six base-36 characters, about 2.2 billion values per
     * source. That sounds ample and is not: by the birthday bound a collision
     * becomes likely around 55 000 events for one source, and is already possible
     * in the low thousands. A collision overwrote the earlier event silently
     * (#27), which on an append-only store is the worst available outcome.
     */
    record Id(String prefix, String instance) {

        private static final SecureRandom RANDOM = new SecureRandom();

        /** Canonical UUID form, any version: what this accepts on the way in. */
        private static final String UUID_FORMAT =
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

        public Id {
            Objects.requireNonNull(prefix);
            if (!prefix.matches("[a-z]+")) {
                throw new IllegalArgumentException("prefix must be lowercase characters");
            }

            Objects.requireNonNull(instance);
            if (!instance.matches(UUID_FORMAT)) {
                throw new IllegalArgumentException(
                        "instance must be a lowercase canonical UUID, got: " + instance);
            }
        }

        /**
         * Splits on the first separator only: the instance is a UUID and carries
         * four hyphens of its own.
         */
        public static Id fromString(String id) {
            Objects.requireNonNull(id);
            int separator = id.indexOf('-');
            if (separator < 0) {
                throw new IllegalArgumentException("Invalid id format: " + id);
            }
            return new Id(id.substring(0, separator), id.substring(separator + 1));
        }

        @Override
        public String toString() {
            return prefix + "-" + instance;
        }

        public static Id generate(String prefix) {
            return new Id(prefix, uuidV7().toString());
        }

        /**
         * A UUID version 7 (RFC 9562): 48 bits of Unix milliseconds, then random.
         *
         * <p>Hand-rolled because the JDK only generates version 4 and
         * {@code nexus-domain} takes no third-party dependency — a UUID library
         * here would be imposed on every plugin author (CLAUDE.md rule 2).
         *
         * <p>Two ids minted within the same millisecond order arbitrarily between
         * themselves; the RFC allows this, and nothing here needs a total order
         * finer than the millisecond.
         */
        private static UUID uuidV7() {
            long timestamp = System.currentTimeMillis();

            // 48 bits of timestamp, 4 bits of version, 12 random bits.
            long high = (timestamp & 0xFFFF_FFFF_FFFFL) << 16
                    | 0x7000L
                    | (RANDOM.nextLong() & 0x0FFFL);

            // Variant 10xx, then 62 random bits.
            long low = 0x8000_0000_0000_0000L
                    | (RANDOM.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL);

            return new UUID(high, low);
        }

    }

    Id id();

    Context context();

    Instant timestamp();

}
