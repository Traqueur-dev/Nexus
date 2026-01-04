package fr.traqueur.nexus.core.interfaces.rest;

import fr.traqueur.nexus.core.application.mapper.EventMapper;
import fr.traqueur.nexus.core.application.services.EventService;
import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.interfaces.rest.dto.EventRequestDto;
import fr.traqueur.nexus.core.interfaces.rest.dto.EventResponseDto;
import fr.traqueur.nexus.core.interfaces.rest.exceptions.EventNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;
    private final EventMapper eventMapper;

    public EventController(EventService eventService, EventMapper eventMapper) {
        this.eventService = eventService;
        this.eventMapper = eventMapper;
    }

    @PostMapping
    public ResponseEntity<String> createEvent(@RequestBody EventRequestDto eventRequestDto){
        Event event = eventMapper.toDomain(eventRequestDto);
        eventService.save(event);
        return ResponseEntity.created(URI.create("/api/v1/events/" + event.id())).body(event.id().toString());
    }

    @GetMapping("/{id}")
    public EventResponseDto getEvent(@PathVariable String id){
        Optional<Event> event = eventService.findById(id);
        return event.map(eventMapper::toDto).orElseThrow(() -> new EventNotFoundException("Event not found with id: " + id));
    }

}
