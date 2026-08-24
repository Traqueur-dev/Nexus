package fr.traqueur.nexus.core.application.services;

import fr.traqueur.nexus.core.application.events.EventFactory;
import fr.traqueur.nexus.core.application.ports.in.IngestEvent;
import fr.traqueur.nexus.core.application.ports.in.IngestEventCommand;
import fr.traqueur.nexus.core.application.ports.out.EventRepository;
import fr.traqueur.nexus.core.domain.events.Event;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EventService implements IngestEvent {

    private final EventRepository events;
    private final EventFactory factory;

    public EventService(EventRepository events, EventFactory factory) {
        this.events = events;
        this.factory = factory;
    }

    @Override
    public Event ingest(IngestEventCommand command) {
        Event.Id id = Event.Id.generate(command.source());
        Event event = factory.create(
                command.type(), id, command.context(), command.timestamp(), command.payload());
        events.save(event);
        return event;
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
