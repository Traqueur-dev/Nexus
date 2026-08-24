package fr.traqueur.nexus.domain.events.internal;

import fr.traqueur.nexus.domain.events.Event;

public interface InternalEvent extends Event {

    @Override
    InternalContext context();

}
