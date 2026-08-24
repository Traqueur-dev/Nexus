package fr.traqueur.nexus.domain.events.discord;

import fr.traqueur.nexus.domain.events.Event;

public interface DiscordEvent extends Event {

    @Override
    DiscordContext context();

}
