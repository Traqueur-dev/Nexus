package fr.traqueur.nexus.core.domain.events.github;

import fr.traqueur.nexus.core.domain.events.Context;

public record GitHubContext() implements Context {
    @Override
    public String source() {
        return "github";
    }
}
