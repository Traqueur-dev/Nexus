package fr.traqueur.nexus.core.application.ports.in;

import fr.traqueur.nexus.core.domain.events.Event;

/**
 * Inbound port: hand Nexus an event that happened somewhere.
 *
 * <p>Implemented by the application, called by driving adapters — today the
 * RabbitMQ consumer, tomorrow a REST endpoint or a plugin.
 */
public interface IngestEvent {

    /**
     * Assigns an identifier, builds the event and stores it.
     *
     * @return the stored event, so callers can log or return its identifier
     */
    Event ingest(IngestEventCommand command);
}
