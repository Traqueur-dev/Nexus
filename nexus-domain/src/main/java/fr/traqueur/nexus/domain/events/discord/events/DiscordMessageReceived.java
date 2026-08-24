package fr.traqueur.nexus.domain.events.discord.events;

import fr.traqueur.nexus.domain.events.EventMetadata;
import fr.traqueur.nexus.domain.events.discord.DiscordContext;
import fr.traqueur.nexus.domain.events.discord.DiscordEvent;

import java.time.Instant;

@EventMetadata(type="discord.message_received")
public record DiscordMessageReceived(Id id, DiscordContext context, Instant timestamp,
                                     String content, long authorId) implements DiscordEvent {
}
