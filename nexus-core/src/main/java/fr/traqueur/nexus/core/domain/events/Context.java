package fr.traqueur.nexus.core.domain.events;

import fr.traqueur.nexus.core.domain.events.discord.DiscordContext;
import fr.traqueur.nexus.core.domain.events.github.GitHubContext;
import fr.traqueur.nexus.core.domain.events.internal.InternalContext;

public sealed interface Context permits DiscordContext, GitHubContext, InternalContext {

    String source();

}
