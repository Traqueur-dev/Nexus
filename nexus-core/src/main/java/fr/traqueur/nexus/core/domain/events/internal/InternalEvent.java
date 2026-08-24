package fr.traqueur.nexus.core.domain.events.internal;

import fr.traqueur.nexus.core.domain.events.Event;

public interface InternalEvent extends Event {

    @Override
    InternalContext context();

}
