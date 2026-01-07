package fr.traqueur.nexus.core.application.registry;

import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.EventMetadata;
import fr.traqueur.nexus.core.domain.events.discord.events.DiscordMessageReceived;
import fr.traqueur.nexus.core.domain.events.github.events.GitHubPushReceived;
import fr.traqueur.nexus.core.domain.events.internal.events.ScheduledEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventRegistryTest {

    private Registry<Event, EventMetadata> registry;

    @BeforeEach
    void setUp() {
        registry = new Registry<>(Event.class, EventMetadata.class, EventMetadata::type);
    }

    @Nested
    @DisplayName("getClassForType")
    class GetClassForType {

        @Test
        @DisplayName("should return DiscordMessageReceived for discord.message_received")
        void shouldReturnDiscordMessageReceived() {
            Class<? extends Event> eventClass = registry.getClassForType("discord.message_received");

            assertThat(eventClass).isEqualTo(DiscordMessageReceived.class);
        }

        @Test
        @DisplayName("should return GitHubPushReceived for github.push_received")
        void shouldReturnGitHubPushReceived() {
            Class<? extends Event> eventClass = registry.getClassForType("github.push_received");

            assertThat(eventClass).isEqualTo(GitHubPushReceived.class);
        }

        @Test
        @DisplayName("should return ScheduledEvent for internal.scheduled_event")
        void shouldReturnScheduledEvent() {
            Class<? extends Event> eventClass = registry.getClassForType("internal.scheduled_event");

            assertThat(eventClass).isEqualTo(ScheduledEvent.class);
        }

        @Test
        @DisplayName("should return null for unknown type")
        void shouldReturnNullForUnknownType() {
            Class<? extends Event> eventClass = registry.getClassForType("unknown.event");

            assertThat(eventClass).isNull();
        }
    }

    @Nested
    @DisplayName("getTypeForClass")
    class GetTypeForClass {

        @Test
        @DisplayName("should return discord.message_received for DiscordMessageReceived")
        void shouldReturnDiscordType() {
            String type = registry.getTypeForClass(DiscordMessageReceived.class);

            assertThat(type).isEqualTo("discord.message_received");
        }

        @Test
        @DisplayName("should return github.push_received for GitHubPushReceived")
        void shouldReturnGitHubType() {
            String type = registry.getTypeForClass(GitHubPushReceived.class);

            assertThat(type).isEqualTo("github.push_received");
        }

        @Test
        @DisplayName("should return internal.scheduled_event for ScheduledEvent")
        void shouldReturnInternalType() {
            String type = registry.getTypeForClass(ScheduledEvent.class);

            assertThat(type).isEqualTo("internal.scheduled_event");
        }
    }

    @Nested
    @DisplayName("discovery")
    class Discovery {

        @Test
        @DisplayName("should discover all annotated events")
        void shouldDiscoverAllEvents() {
            // Verify bidirectional mapping works for all discovered events
            assertThat(registry.getClassForType("discord.message_received")).isNotNull();
            assertThat(registry.getClassForType("github.push_received")).isNotNull();
            assertThat(registry.getClassForType("internal.scheduled_event")).isNotNull();
        }
    }
}