package fr.traqueur.nexus.domain.events.internal.events;

import fr.traqueur.nexus.domain.events.EventMetadata;
import fr.traqueur.nexus.domain.events.internal.InternalContext;
import fr.traqueur.nexus.domain.events.internal.InternalEvent;

import java.time.Instant;

@EventMetadata(type="internal.scheduled_event")
public record ScheduledEvent(Id id, InternalContext context, Instant timestamp, String cronExpression, String taskName) implements InternalEvent {
}
