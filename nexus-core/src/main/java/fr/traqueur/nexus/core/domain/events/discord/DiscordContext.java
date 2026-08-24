package fr.traqueur.nexus.core.domain.events.discord;

import fr.traqueur.nexus.core.domain.events.Context;
import fr.traqueur.nexus.core.domain.events.ContextMetadata;

@ContextMetadata(type = "discord")
public record DiscordContext() implements Context {
}
