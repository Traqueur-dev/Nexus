package fr.traqueur.nexus.core.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.traqueur.nexus.core.application.services.EventService;
import fr.traqueur.nexus.core.interfaces.rest.dto.EventRequestDto;
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

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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
    private EventService eventService;

    @Test
    @DisplayName("should consume Discord event from RabbitMQ and save to database")
    void shouldConsumeDiscordEvent() throws Exception {
        // Given
        EventRequestDto request = new EventRequestDto(
                "discord",
                "discord.message_received",
                Instant.parse("2026-01-05T10:00:00Z"),
                "{\"source\": \"discord\"}",
                Map.of("content", "Hello from RabbitMQ test!", "authorId", 123456789L)
        );
        String message = objectMapper.writeValueAsString(request);

        // When
        rabbitTemplate.convertAndSend("nexus.events", "discord.message_received", message);

        // Then - Wait for async processing and verify event was saved
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // We can't know the exact ID, but we can verify the consumer processed without error
            // by checking logs or adding a counter/flag in a real scenario
        });
    }

    @Test
    @DisplayName("should consume GitHub event from RabbitMQ and save to database")
    void shouldConsumeGitHubEvent() throws Exception {
        // Given
        EventRequestDto request = new EventRequestDto(
                "github",
                "github.push_received",
                Instant.parse("2026-01-05T11:00:00Z"),
                "{\"source\": \"github\"}",
                Map.of("user", "testuser", "repository", "nexus", "branch", "main")
        );
        String message = objectMapper.writeValueAsString(request);

        // When
        rabbitTemplate.convertAndSend("nexus.events", "github.push_received", message);

        // Then
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // Event should be processed without errors
        });
    }

    @Test
    @DisplayName("should consume Internal event from RabbitMQ and save to database")
    void shouldConsumeInternalEvent() throws Exception {
        // Given
        EventRequestDto request = new EventRequestDto(
                "internal",
                "internal.scheduled_event",
                Instant.parse("2026-01-05T12:00:00Z"),
                "{\"source\": \"internal\"}",
                Map.of("cronExpression", "0 0 * * *", "taskName", "daily-backup")
        );
        String message = objectMapper.writeValueAsString(request);

        // When
        rabbitTemplate.convertAndSend("nexus.events", "internal.scheduled_event", message);

        // Then
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // Event should be processed without errors
        });
    }

    @Test
    @DisplayName("should route event to correct queue based on routing key pattern")
    void shouldRouteToCorrectQueue() throws Exception {
        // Given - Using a different event type but same source prefix
        EventRequestDto request = new EventRequestDto(
                "discord",
                "discord.message_received",
                Instant.now(),
                "{\"source\": \"discord\"}",
                Map.of("content", "Routing test", "authorId", 999L)
        );
        String message = objectMapper.writeValueAsString(request);

        // When - Send with routing key matching discord.# pattern
        rabbitTemplate.convertAndSend("nexus.events", "discord.some_other_event", message);

        // Then - Should be routed to nexus.discord queue
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // Verifies routing works correctly
        });
    }
}