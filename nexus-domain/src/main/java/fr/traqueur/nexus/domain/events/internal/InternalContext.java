package fr.traqueur.nexus.domain.events.internal;

import fr.traqueur.nexus.domain.events.Context;
import fr.traqueur.nexus.domain.events.ContextMetadata;

@ContextMetadata(type = "internal")
public record InternalContext() implements Context {
}
