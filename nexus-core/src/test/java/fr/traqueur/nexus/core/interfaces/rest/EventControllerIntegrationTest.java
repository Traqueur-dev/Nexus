package fr.traqueur.nexus.core.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.traqueur.nexus.core.application.services.EventService;
import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.infrastructure.serialization.JacksonConfig;
import fr.traqueur.nexus.core.interfaces.rest.dto.EventRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@Import(JacksonConfig.class)
class EventControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:4-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private EventService eventService;

    @Nested
    @DisplayName("GET /api/v1/events/{id}")
    class GetEvent {

        @Test
        @DisplayName("should retrieve event created via RabbitMQ")
        void shouldRetrieveCreatedEvent() throws Exception {
            // Given - Send event via RabbitMQ
            EventRequestDto request = new EventRequestDto(
                    "discord",
                    "discord.message_received",
                    Instant.parse("2026-01-04T12:30:00Z"),
                    "{\"source\": \"discord\"}",
                    Map.of("content", "Test message", "authorId", 111222333L)
            );
            String message = objectMapper.writeValueAsString(request);

            rabbitTemplate.convertAndSend("nexus.events", "discord.message_received", message);

            // Wait for async processing
            await().atMost(5, TimeUnit.SECONDS).until(() ->
                    eventService.findLatestBySource("discord").isPresent()
            );

            Event event = eventService.findLatestBySource("discord").get();

            // When & Then - Retrieve via REST
            mockMvc.perform(get("/api/v1/events/{id}", event.id().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(event.id().toString()))
                    .andExpect(jsonPath("$.source").value("discord"))
                    .andExpect(jsonPath("$.type").value("discord.message_received"))
                    .andExpect(jsonPath("$.payload.content").value("Test message"));
        }

        @Test
        @DisplayName("should return 404 for non-existent event")
        void shouldReturn404ForNonExistentEvent() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/events/{id}", "unknown-abc123"))
                    .andExpect(status().isNotFound());
        }
    }

}