package fr.traqueur.nexus.domain.events.github;

import fr.traqueur.nexus.domain.events.Context;
import fr.traqueur.nexus.domain.events.ContextMetadata;

@ContextMetadata(type = "github")
public record GitHubContext() implements Context {
}
