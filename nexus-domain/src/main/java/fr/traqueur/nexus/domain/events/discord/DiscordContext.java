package fr.traqueur.nexus.domain.events.discord;

import fr.traqueur.nexus.domain.events.Context;
import fr.traqueur.nexus.domain.events.ContextMetadata;

@ContextMetadata(type = "discord")
public record DiscordContext() implements Context {
}
