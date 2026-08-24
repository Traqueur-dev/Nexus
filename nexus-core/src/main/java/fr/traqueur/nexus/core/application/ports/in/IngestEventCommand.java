package fr.traqueur.nexus.core.application.ports.in;

import fr.traqueur.nexus.core.domain.events.Context;
import fr.traqueur.nexus.core.domain.events.Event;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * The contract for ingesting an event, whatever the transport.
 *
 * <p>This used to be {@code interfaces.rest.dto.EventRequestDto}, which meant the
 * RabbitMQ consumer imported a REST DTO — two independent adapters coupled through
 * a type that belonged to neither. Ingesting an event is an application concern,
 * so the contract lives here and each adapter maps its own payload onto it.
 *
 * <p>The context arrives already decoded: turning a wire format into a domain type
 * is the adapter's job, not the application's.
 *
 * @param source    the source name, also used as the {@link Event.Id} prefix
 * @param type      the registered event type identifier, e.g. {@code github.push_received}
 * @param timestamp when the event occurred at its source
 * @param context   source-specific metadata
 * @param payload   the remaining event fields, keyed by record component name
 */
public record IngestEventCommand(
        String source,
        String type,
        Instant timestamp,
        Context context,
        Map<String, Object> payload
) {

    public IngestEventCommand {
        Objects.requireNonNull(source, "source is required");
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(timestamp, "timestamp is required");
        Objects.requireNonNull(context, "context is required");
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
