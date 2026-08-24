package fr.traqueur.nexus.infrastructure.rest.dto;

import fr.traqueur.nexus.domain.events.Context;

import java.time.Instant;
import java.util.Map;

/**
 * The REST representation of an event.
 *
 * <p>{@code context} is typed as the domain type, not as a pre-encoded
 * {@code String}. Encoding it here meant the message converter then encoded that
 * string again, so clients received the context as an escaped JSON blob they had
 * to parse a second time. Handing the converter the object lets the one
 * configured mapper write it â€” the same shape it has everywhere else.
 */
public record EventResponseDto(
        String id,
        String source,
        String type,
        Instant timestamp,
        Context context,
        Map<String, Object> payload
) {
}