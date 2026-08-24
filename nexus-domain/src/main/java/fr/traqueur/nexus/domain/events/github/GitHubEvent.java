package fr.traqueur.nexus.domain.events.github;

import fr.traqueur.nexus.domain.events.Event;

public interface GitHubEvent extends Event {

    @Override
    GitHubContext context();

}
