package fr.traqueur.nexus.infrastructure.rest;

import fr.traqueur.nexus.application.ports.in.QueryEvents;
import fr.traqueur.nexus.domain.events.Event;
import fr.traqueur.nexus.infrastructure.rest.dto.EventResponseDto;
import fr.traqueur.nexus.infrastructure.rest.exceptions.EventNotFoundException;
import fr.traqueur.nexus.infrastructure.rest.exceptions.InvalidEventIdException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final QueryEvents events;
    private final EventDtoMapper eventDtoMapper;

    /**
     * Depends on the inbound port, not on {@code EventService}: the adapter states
     * what it needs from the application — reading events — and nothing more.
     * Injecting the service handed the HTTP layer the ingestion path too.
     */
    public EventController(QueryEvents events, EventDtoMapper eventDtoMapper) {
        this.events = events;
        this.eventDtoMapper = eventDtoMapper;
    }

    @GetMapping("/{id}")
    public EventResponseDto getEvent(@PathVariable String id) {
        Event.Id eventId = parseId(id);
        return events.findById(eventId)
                .map(eventDtoMapper::toDto)
                .orElseThrow(() -> new EventNotFoundException("Event not found with id: " + id));
    }

    /**
     * Translates the raw path variable into the domain's identifier.
     *
     * <p>Parsing at the edge keeps the malformed-input concern in the adapter that
     * received it, and means everything further in only ever handles a validated
     * {@link Event.Id}.
     */
    private Event.Id parseId(String raw) {
        try {
            return Event.Id.fromString(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidEventIdException(raw, e);
        }
    }

}
