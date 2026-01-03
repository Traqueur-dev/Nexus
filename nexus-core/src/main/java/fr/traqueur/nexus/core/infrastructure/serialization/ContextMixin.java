package fr.traqueur.nexus.core.infrastructure.serialization;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import fr.traqueur.nexus.core.domain.events.discord.DiscordContext;
import fr.traqueur.nexus.core.domain.events.github.GitHubContext;
import fr.traqueur.nexus.core.domain.events.internal.InternalContext;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "source")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DiscordContext.class, name = "discord"),
    @JsonSubTypes.Type(value = GitHubContext.class, name = "github"),
    @JsonSubTypes.Type(value = InternalContext.class, name = "internal")
})
public abstract class ContextMixin {}