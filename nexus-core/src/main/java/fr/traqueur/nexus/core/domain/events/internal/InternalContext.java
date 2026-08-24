package fr.traqueur.nexus.core.domain.events.internal;

import fr.traqueur.nexus.core.domain.events.Context;
import fr.traqueur.nexus.core.domain.events.ContextMetadata;

@ContextMetadata(type = "internal")
public record InternalContext() implements Context {
}
