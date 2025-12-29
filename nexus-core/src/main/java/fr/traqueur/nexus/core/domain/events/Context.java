package fr.traqueur.nexus.core.domain.events;

import fr.traqueur.nexus.core.domain.events.discord.DiscordContext;

public sealed interface Context permits DiscordContext {

    String source();

}
