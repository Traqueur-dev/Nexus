package fr.traqueur.nexus.infrastructure.messaging;

import fr.traqueur.nexus.application.ports.out.EventRepository;
import fr.traqueur.nexus.domain.events.Event;
import fr.traqueur.nexus.domain.events.discord.events.DiscordMessageReceived;
import fr.traqueur.nexus.domain.events.github.events.GitHubPushReceived;
import fr.traqueur.nexus.domain.events.internal.events.ScheduledEvent;
import fr.traqueur.nexus.infrastructure.messaging.dto.EventMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Covers the RabbitMQ ingestion path end to end: message in, event readable back.
 *
 * <p>Every test asserts the stored event, not just that the consumer did not throw.
 * The earlier version waited on an empty {@code untilAsserted} block, so all four
 * tests passed while the consumer was storing contexts that could not be read
 * back at all — a green suite over a broken path.
 *
 * <p>Each message carries {@code Instant.now()}, which makes it the latest of its
 * source whatever order the methods run in. Reading it back through
 * {@code findLatestBySource} is what proves the round-trip, since the id is
 * generated during ingestion and the sender never learns it.
 */
@SpringBootTest
@Testcontainers
class EventConsumerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:4-alpine");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Test
    @DisplayName("should consume Discord event from RabbitMQ and save to database")
    void shouldConsumeDiscordEvent() {
        publish("discord", "discord.message_received", "{\"source\": \"discord\"}",
                Map.of("content", "Hello from RabbitMQ test!", "authorId", 123456789L),
                "discord.message_received");

        DiscordMessageReceived stored = awaitLatest("discord", DiscordMessageReceived.class);

        assertThat(stored.content()).isEqualTo("Hello from RabbitMQ test!");
        assertThat(stored.authorId()).isEqualTo(123456789L);
        assertThat(stored.context().source()).isEqualTo("discord");
    }

    @Test
    @DisplayName("should consume GitHub event from RabbitMQ and save to database")
    void shouldConsumeGitHubEvent() {
        publish("github", "github.push_received", "{\"source\": \"github\"}",
                Map.of("user", "testuser", "repository", "nexus", "branch", "main"),
                "github.push_received");

        GitHubPushReceived stored = awaitLatest("github", GitHubPushReceived.class);

        assertThat(stored.user()).isEqualTo("testuser");
        assertThat(stored.repository()).isEqualTo("nexus");
        assertThat(stored.branch()).isEqualTo("main");
    }

    @Test
    @DisplayName("should consume Internal event from RabbitMQ and save to database")
    void shouldConsumeInternalEvent() {
        publish("internal", "internal.scheduled_event", "{\"source\": \"internal\"}",
                Map.of("cronExpression", "0 0 * * *", "taskName", "daily-backup"),
                "internal.scheduled_event");

        ScheduledEvent stored = awaitLatest("internal", ScheduledEvent.class);

        assertThat(stored.cronExpression()).isEqualTo("0 0 * * *");
        assertThat(stored.taskName()).isEqualTo("daily-backup");
    }

    @Test
    @DisplayName("should route event to correct queue based on routing key pattern")
    void shouldRouteToCorrectQueue() {
        // Routing key differs from the event type: what binds the message to the
        // nexus.discord queue is the discord.# pattern, not the type it carries.
        publish("discord", "discord.message_received", "{\"source\": \"discord\"}",
                Map.of("content", "Routing test", "authorId", 999L),
                "discord.some_other_event");

        DiscordMessageReceived stored = awaitLatest("discord", DiscordMessageReceived.class);

        assertThat(stored.content()).isEqualTo("Routing test");
    }

    private void publish(String source, String type, String context, Map<String, Object> payload, String routingKey) {
        EventMessage message = new EventMessage(source, type, Instant.now(), context, payload);
        rabbitTemplate.convertAndSend("nexus.events", routingKey, objectMapper.writeValueAsString(message));
    }

    private <T extends Event> T awaitLatest(String source, Class<T> type) {
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<Event> latest = eventRepository.findLatestBySource(source);
            assertThat(latest).isPresent();
            assertThat(latest.get()).isInstanceOf(type);
        });
        return type.cast(eventRepository.findLatestBySource(source).orElseThrow());
    }
}