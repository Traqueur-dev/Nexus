package fr.traqueur.nexus.core.domain.events.discord;

import fr.traqueur.nexus.core.domain.events.Context;

public record DiscordContext() implements Context {

    @Override
    public String source() {
        return "discord";
    }
}
