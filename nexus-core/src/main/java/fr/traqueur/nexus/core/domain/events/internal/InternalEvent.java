package fr.traqueur.nexus.core.domain.events.internal;

import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.internal.events.ScheduledEvent;

public sealed interface InternalEvent extends Event permits ScheduledEvent {

    @Override
    InternalContext context();

}
