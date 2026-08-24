package fr.traqueur.nexus.core.domain.events.discord;

import fr.traqueur.nexus.core.domain.events.Event;

public interface DiscordEvent extends Event {

    @Override
    DiscordContext context();

}
