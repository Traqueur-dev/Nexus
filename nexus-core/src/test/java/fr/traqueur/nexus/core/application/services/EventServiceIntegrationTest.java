package fr.traqueur.nexus.core.application.services;

import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.discord.DiscordContext;
import fr.traqueur.nexus.core.domain.events.discord.events.DiscordMessageReceived;
import fr.traqueur.nexus.core.domain.events.github.GitHubContext;
import fr.traqueur.nexus.core.domain.events.github.events.GitHubPushReceived;
import fr.traqueur.nexus.core.domain.events.internal.InternalContext;
import fr.traqueur.nexus.core.domain.events.internal.events.ScheduledEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class EventServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:4-alpine");

    @Autowired
    private EventService eventService;

    @Test
    @DisplayName("should save and retrieve DiscordMessageReceived")
    void shouldSaveAndRetrieveDiscordEvent() {
        // Given
        Event.Id id = Event.Id.generate("disc");
        DiscordContext context = new DiscordContext();
        Instant timestamp = Instant.now();
        DiscordMessageReceived event = new DiscordMessageReceived(
                id, context, timestamp, "Hello from test!", 987654321L
        );

        // When
        eventService.save(event);
        Optional<Event> retrieved = eventService.findById(id.toString());

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isInstanceOf(DiscordMessageReceived.class);

        DiscordMessageReceived retrievedEvent = (DiscordMessageReceived) retrieved.get();
        assertThat(retrievedEvent.id().toString()).isEqualTo(id.toString());
        assertThat(retrievedEvent.content()).isEqualTo("Hello from test!");
        assertThat(retrievedEvent.authorId()).isEqualTo(987654321L);
        assertThat(retrievedEvent.context().source()).isEqualTo("discord");
    }

    @Test
    @DisplayName("should save and retrieve GitHubPushReceived")
    void shouldSaveAndRetrieveGitHubEvent() {
        // Given
        Event.Id id = Event.Id.generate("gh");
        GitHubContext context = new GitHubContext();
        Instant timestamp = Instant.now();
        GitHubPushReceived event = new GitHubPushReceived(
                id, context, timestamp, "testuser", "my-repo", "develop"
        );

        // When
        eventService.save(event);
        Optional<Event> retrieved = eventService.findById(id.toString());

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isInstanceOf(GitHubPushReceived.class);

        GitHubPushReceived retrievedEvent = (GitHubPushReceived) retrieved.get();
        assertThat(retrievedEvent.user()).isEqualTo("testuser");
        assertThat(retrievedEvent.repository()).isEqualTo("my-repo");
        assertThat(retrievedEvent.branch()).isEqualTo("develop");
    }

    @Test
    @DisplayName("should save and retrieve ScheduledEvent")
    void shouldSaveAndRetrieveInternalEvent() {
        // Given
        Event.Id id = Event.Id.generate("int");
        InternalContext context = new InternalContext();
        Instant timestamp = Instant.now();
        ScheduledEvent event = new ScheduledEvent(
                id, context, timestamp, "*/5 * * * *", "health-check"
        );

        // When
        eventService.save(event);
        Optional<Event> retrieved = eventService.findById(id.toString());

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isInstanceOf(ScheduledEvent.class);

        ScheduledEvent retrievedEvent = (ScheduledEvent) retrieved.get();
        assertThat(retrievedEvent.cronExpression()).isEqualTo("*/5 * * * *");
        assertThat(retrievedEvent.taskName()).isEqualTo("health-check");
    }

    @Test
    @DisplayName("should return empty when event not found")
    void shouldReturnEmptyWhenNotFound() {
        // When
        Optional<Event> retrieved = eventService.findById("unknown-abc123");

        // Then
        assertThat(retrieved).isEmpty();
    }

    @Test
    @DisplayName("should preserve timestamp through persistence")
    void shouldPreserveTimestamp() {
        // Given
        Event.Id id = Event.Id.generate("disc");
        Instant timestamp = Instant.parse("2026-01-03T12:30:45.123456Z");
        DiscordMessageReceived event = new DiscordMessageReceived(
                id, new DiscordContext(), timestamp, "Test", 123L
        );

        // When
        eventService.save(event);
        Optional<Event> retrieved = eventService.findById(id.toString());

        // Then
        assertThat(retrieved).isPresent();
        // Note: PostgreSQL TIMESTAMP may lose some precision
        assertThat(retrieved.get().timestamp().getEpochSecond())
                .isEqualTo(timestamp.getEpochSecond());
    }
}