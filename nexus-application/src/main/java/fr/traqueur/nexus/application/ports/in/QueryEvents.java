package fr.traqueur.nexus.application.ports.in;

import fr.traqueur.nexus.domain.events.Event;

import java.util.Optional;

/**
 * Inbound port: read events Nexus has already ingested.
 *
 * <p>Separate from {@link IngestEvent} because the two answer to different
 * callers. A driving adapter that only reads — the REST endpoint — has no
 * business being able to ingest, and a plugin embedding the query side should
 * not have to accept the write side with it.
 *
 * <p>Deliberately offers no {@code save}: storing an event without running its
 * workflows is not a use case, it is a way to end up with events nobody reacted
 * to. Everything that enters the system goes through {@link IngestEvent}.
 */
public interface QueryEvents {

    Optional<Event> findById(Event.Id id);

    Optional<Event> findLatestBySource(String source);
}