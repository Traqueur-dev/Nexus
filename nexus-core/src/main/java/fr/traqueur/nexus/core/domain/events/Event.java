package fr.traqueur.nexus.core.domain.events;

import fr.traqueur.nexus.core.domain.events.discord.DiscordEvent;
import fr.traqueur.nexus.core.domain.events.github.GitHubEvent;
import fr.traqueur.nexus.core.domain.events.internal.InternalEvent;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;

public sealed interface Event permits DiscordEvent, GitHubEvent, InternalEvent {

    record Id(String prefix, String instance) {

        private static final SecureRandom RANDOM = new SecureRandom();
        private static final String CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

        public Id {
            Objects.requireNonNull(prefix);
            if(!prefix.matches("[a-z]+")) {
                throw new IllegalArgumentException("prefix must be lowercase characters");
            }

            Objects.requireNonNull(instance);
            if(!instance.matches("[a-z0-9]{6}")) {
                throw new IllegalArgumentException("instance must be exactly 6 lowercase alphanumeric characters");
            }
        }

        @Override
        public String toString() {
            return String.format("%s-%s", prefix, instance);
        }

        public static Id generate(String prefix) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                int index = RANDOM.nextInt(CHARS.length());
                char character = CHARS.charAt(index);
                builder.append(character);
            }
            return new Id(prefix, builder.toString());
        }

    }

    Id id();

    Context context();

    Instant timestamp();

}
