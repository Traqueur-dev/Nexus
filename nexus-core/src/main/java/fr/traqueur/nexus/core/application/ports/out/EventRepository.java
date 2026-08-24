package fr.traqueur.nexus.core.application.ports.out;

import fr.traqueur.nexus.core.domain.events.Event;

import java.util.Optional;

/**
 * Outbound port for event storage.
 *
 * <p>The application declares what it needs; an adapter provides it. Nothing here
 * mentions JPA, SQL or Spring Data — swapping the storage technology is an
 * adapter change, and the application layer can be tested against an in-memory
 * implementation with no database running.
 *
 * <p>The port speaks the domain's language: {@link Event.Id}, not {@code String}.
 * A value object unwrapped at the boundary provides no safety, since callers
 * would be free to pass any string through.
 */
public interface EventRepository {

    /**
     * Persists an event.
     *
     * <p>Events are immutable once ingested and the store is append-only, so this
     * is an insert rather than an upsert. Note that the current JPA adapter does
     * not enforce that — see issue #27.
     */
    void save(Event event);

    Optional<Event> findById(Event.Id id);

    Optional<Event> findLatestBySource(String source);
}
