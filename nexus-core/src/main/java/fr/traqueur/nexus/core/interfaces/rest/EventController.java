package fr.traqueur.nexus.core.interfaces.rest;

import fr.traqueur.nexus.core.application.mapper.EventMapper;
import fr.traqueur.nexus.core.application.services.EventService;
import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.interfaces.rest.dto.EventResponseDto;
import fr.traqueur.nexus.core.interfaces.rest.exceptions.EventNotFoundException;
import fr.traqueur.nexus.core.interfaces.rest.exceptions.InvalidEventIdException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;
    private final EventMapper eventMapper;

    public EventController(EventService eventService, EventMapper eventMapper) {
        this.eventService = eventService;
        this.eventMapper = eventMapper;
    }

    @GetMapping("/{id}")
    public EventResponseDto getEvent(@PathVariable String id) {
        Event.Id eventId = parseId(id);
        return eventService.findById(eventId)
                .map(eventMapper::toDto)
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
