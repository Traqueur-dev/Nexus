package fr.traqueur.nexus.core.application.ports.in;

/**
 * Inbound port: hand Nexus an event that happened somewhere.
 *
 * <p>Implemented by the application, called by driving adapters — today the
 * RabbitMQ consumer, tomorrow a REST endpoint or a plugin.
 */
public interface IngestEvent {

    /**
     * Assigns an identifier, builds the event, stores it, then runs the workflows
     * that react to it.
     *
     * @return the stored event and what its workflows did
     */
    IngestionResult ingest(IngestEventCommand command);
}
