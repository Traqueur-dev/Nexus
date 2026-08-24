package fr.traqueur.nexus.application.services;

import fr.traqueur.nexus.application.events.EventFactory;
import fr.traqueur.nexus.application.ports.in.IngestEvent;
import fr.traqueur.nexus.application.ports.in.IngestEventCommand;
import fr.traqueur.nexus.application.ports.in.IngestionResult;
import fr.traqueur.nexus.application.ports.in.QueryEvents;
import fr.traqueur.nexus.application.ports.out.EventRepository;
import fr.traqueur.nexus.application.workflow.WorkflowEngine;
import fr.traqueur.nexus.application.workflow.WorkflowRun;
import fr.traqueur.nexus.domain.events.Event;
import fr.traqueur.nexus.domain.events.EventType;

import java.util.List;
import java.util.Optional;

public class EventService implements IngestEvent, QueryEvents {

    private final EventRepository events;
    private final EventFactory factory;
    private final WorkflowEngine workflows;

    public EventService(EventRepository events, EventFactory factory, WorkflowEngine workflows) {
        this.events = events;
        this.factory = factory;
        this.workflows = workflows;
    }

    /**
     * Ingesting an event means storing it <em>and</em> reacting to it — a stored
     * event nobody acted on is not what the caller asked for. Running the
     * workflows here rather than leaving it to each adapter is what stops the
     * next adapter from forgetting.
     *
     * <p>The event is stored before the workflows run: the record of what
     * happened must survive even if reacting to it fails.
     */
    @Override
    public IngestionResult ingest(IngestEventCommand command) {
        Event.Id id = Event.Id.generate(command.source());
        Event event = factory.create(
                command.type(), id, command.context(), command.timestamp(), command.payload());
        events.save(event);

        List<WorkflowRun> runs = workflows.run(EventType.of(command.type()), event);
        return new IngestionResult(event, runs);
    }

    @Override
    public Optional<Event> findById(Event.Id id) {
        return events.findById(id);
    }

    @Override
    public Optional<Event> findLatestBySource(String source) {
        return events.findLatestBySource(source);
    }

}
