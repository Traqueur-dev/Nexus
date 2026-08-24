package fr.traqueur.nexus.infrastructure.messaging.dto;

import java.time.Instant;
import java.util.Map;

/**
 * The wire format of an event published on the queue.
 *
 * <p>Owned by the messaging adapter, which is what previously went wrong: the
 * consumer deserialized a REST DTO. The field layout is unchanged, so existing
 * publishers keep working — {@code context} is still a JSON document carried as
 * a string.
 */
public record EventMessage(
        String source,
        String type,
        Instant timestamp,
        String context,
        Map<String, Object> payload
) {
}
