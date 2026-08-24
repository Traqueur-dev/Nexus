package fr.traqueur.nexus.api.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.traqueur.nexus.application.events.EventFactory;
import fr.traqueur.nexus.domain.events.Event;
import fr.traqueur.nexus.api.rest.dto.EventResponseDto;
import org.springframework.stereotype.Component;

/**
 * Translates a domain event into the REST representation.
 *
 * <p>The other half of the former {@code EventMapper}. A DTO belongs to the
 * adapter that speaks its protocol, and so does the code that produces it.
 */
@Component
public class EventDtoMapper {

    private final EventFactory factory;
    private final ObjectMapper json;

    public EventDtoMapper(EventFactory factory, ObjectMapper json) {
        this.factory = factory;
        this.json = json;
    }

    public EventResponseDto toDto(Event event) {
        return new EventResponseDto(
                event.id().toString(),
                event.context().source(),
                factory.typeOf(event),
                event.timestamp(),
                serialize(event.context()),
                factory.extractPayload(event));
    }

    private String serialize(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize event context", e);
        }
    }
}
