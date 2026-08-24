package fr.traqueur.nexus.core.domain.events.github;

import fr.traqueur.nexus.core.domain.events.Context;
import fr.traqueur.nexus.core.domain.events.ContextMetadata;

@ContextMetadata(type = "github")
public record GitHubContext() implements Context {
}
