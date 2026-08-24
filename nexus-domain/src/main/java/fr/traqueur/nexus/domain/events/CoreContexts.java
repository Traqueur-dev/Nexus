package fr.traqueur.nexus.domain.events;

import fr.traqueur.nexus.domain.events.discord.DiscordContext;
import fr.traqueur.nexus.domain.events.github.GitHubContext;
import fr.traqueur.nexus.domain.events.internal.InternalContext;

import java.util.List;

/**
 * The context types shipped by the core itself.
 *
 * <p>Same rationale as {@link CoreEvents}: with an open hierarchy the set of
 * built-in contexts is declared rather than discovered.
 */
public final class CoreContexts {

    private CoreContexts() {
    }

    public static List<Class<? extends Context>> types() {
        return List.of(
                DiscordContext.class,
                GitHubContext.class,
                InternalContext.class
        );
    }
}
