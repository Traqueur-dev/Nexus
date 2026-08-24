package fr.traqueur.nexus.core.application.services;

import fr.traqueur.nexus.core.application.ports.out.EventRepository;
import fr.traqueur.nexus.core.domain.events.Event;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EventService {

    private final EventRepository events;

    public EventService(EventRepository events) {
        this.events = events;
    }

    public void save(Event event) {
        events.save(event);
    }

    public Optional<Event> findById(Event.Id id) {
        return events.findById(id);
    }

    public Optional<Event> findLatestBySource(String source) {
        return events.findLatestBySource(source);
    }

}
