package fr.traqueur.nexus.api.rest;

import fr.traqueur.nexus.application.services.EventService;
import fr.traqueur.nexus.domain.events.Event;
import fr.traqueur.nexus.api.rest.dto.EventResponseDto;
import fr.traqueur.nexus.api.rest.exceptions.EventNotFoundException;
import fr.traqueur.nexus.api.rest.exceptions.InvalidEventIdException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;
    private final EventDtoMapper eventDtoMapper;

    public EventController(EventService eventService, EventDtoMapper eventDtoMapper) {
        this.eventService = eventService;
        this.eventDtoMapper = eventDtoMapper;
    }

    @GetMapping("/{id}")
    public EventResponseDto getEvent(@PathVariable String id) {
        Event.Id eventId = parseId(id);
        return eventService.findById(eventId)
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
