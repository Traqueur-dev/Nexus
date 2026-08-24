package fr.traqueur.nexus.infrastructure.rest;

import fr.traqueur.nexus.infrastructure.rest.dto.EventResponseDto;
import fr.traqueur.nexus.application.events.EventFactory;
import fr.traqueur.nexus.domain.events.Event;
import org.springframework.stereotype.Component;

/**
 * Translates a domain event into the REST representation.
 *
 * <p>The other half of the former {@code EventMapper}. A DTO belongs to the
 * adapter that speaks its protocol, and so does the code that produces it.
 *
 * <p>It writes no JSON itself: producing the wire format is the message
 * converter's job, which is what keeps this module free of any Jackson
 * dependency.
 */
@Component
public class EventDtoMapper {

    private final EventFactory factory;

    public EventDtoMapper(EventFactory factory) {
        this.factory = factory;
    }

    public EventResponseDto toDto(Event event) {
        return new EventResponseDto(
                event.id().toString(),
                event.context().source(),
                factory.typeOf(event),
                event.timestamp(),
                event.context(),
                factory.extractPayload(event));
    }
}