package fr.traqueur.nexus.application.ports.out;

import fr.traqueur.nexus.domain.events.Event;

/**
 * Raised when an event is written under an identifier that already exists.
 *
 * <p>The event store is append-only: a stored event is never modified. This is
 * what makes that a rule rather than an intention — the write fails and says so,
 * instead of replacing what was there.
 *
 * <p>Part of the port's contract, not an adapter detail, because every
 * implementation owes the caller this guarantee. With UUID version 7 identifiers
 * it should be unreachable; it stays because "should be unreachable" is exactly
 * what was believed about six base-36 characters (#27).
 */
public class EventAlreadyStoredException extends RuntimeException {

    private final Event.Id id;

    public EventAlreadyStoredException(Event.Id id, Throwable cause) {
        super("An event is already stored under id " + id, cause);
        this.id = id;
    }

    public Event.Id id() {
        return id;
    }
}