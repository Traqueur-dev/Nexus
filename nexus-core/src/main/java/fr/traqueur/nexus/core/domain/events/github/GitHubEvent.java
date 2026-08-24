package fr.traqueur.nexus.core.domain.events.github;

import fr.traqueur.nexus.core.domain.events.Event;

public interface GitHubEvent extends Event {

    @Override
    GitHubContext context();

}
