package fr.traqueur.nexus.core.domain.events.github;

import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.github.events.GitHubPushReceived;

public sealed interface GitHubEvent extends Event permits GitHubPushReceived {

    @Override
    GitHubContext context();

}
