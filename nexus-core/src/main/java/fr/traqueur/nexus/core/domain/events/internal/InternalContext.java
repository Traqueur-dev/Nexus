package fr.traqueur.nexus.core.domain.events.internal;

import fr.traqueur.nexus.core.domain.events.Context;

public record InternalContext() implements Context {

    @Override
    public String source() {
        return "internal";
    }

}
