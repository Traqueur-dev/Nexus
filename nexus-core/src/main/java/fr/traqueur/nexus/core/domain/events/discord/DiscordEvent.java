package fr.traqueur.nexus.core.domain.events.discord;

import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.discord.events.DiscordMessageReceived;

public sealed interface DiscordEvent extends Event permits DiscordMessageReceived {

    @Override
    DiscordContext context();

}
