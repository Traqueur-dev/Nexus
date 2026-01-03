package fr.traqueur.nexus.core.domain.events.github.events;

import fr.traqueur.nexus.core.domain.events.EventMetadata;
import fr.traqueur.nexus.core.domain.events.github.GitHubContext;
import fr.traqueur.nexus.core.domain.events.github.GitHubEvent;

import java.time.Instant;

@EventMetadata(type = "github.push_received")
public record GitHubPushReceived(Id id, GitHubContext context, Instant timestamp,
                                 String user,
                                 String repository,
                                 String branch) implements GitHubEvent {
}
