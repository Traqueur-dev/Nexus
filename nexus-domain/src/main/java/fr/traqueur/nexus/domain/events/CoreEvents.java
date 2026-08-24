package fr.traqueur.nexus.domain.events;

import fr.traqueur.nexus.domain.events.discord.events.DiscordMessageReceived;
import fr.traqueur.nexus.domain.events.github.events.GitHubPushReceived;
import fr.traqueur.nexus.domain.events.internal.events.ScheduledEvent;

import java.util.List;

/**
 * The event types shipped by the core itself.
 *
 * <p>With an open hierarchy nothing discovers these automatically any more, so
 * the core declares them the same way an adapter will declare its own. Adding an
 * event type means adding it here — the explicitness is the point: it is the
 * seam a plugin plugs into.
 */
public final class CoreEvents {

    private CoreEvents() {
    }

    public static List<Class<? extends Event>> types() {
        return List.of(
                DiscordMessageReceived.class,
                GitHubPushReceived.class,
                ScheduledEvent.class
        );
    }
}
