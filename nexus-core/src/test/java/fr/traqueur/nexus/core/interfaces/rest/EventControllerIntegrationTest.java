package fr.traqueur.nexus.core.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.traqueur.nexus.core.infrastructure.serialization.JacksonConfig;
import fr.traqueur.nexus.core.interfaces.rest.dto.EventRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("POST /api/v1/events")
    class CreateEvent {

        @Test
        @DisplayName("should create Discord event and return 201")
        void shouldCreateDiscordEvent() throws Exception {
            // Given
            EventRequestDto request = new EventRequestDto(
                    "discord",
                    "discord.message_received",
                    Instant.parse("2026-01-04T10:00:00Z"),
                    "{\"source\": \"discord\"}",
                    Map.of("content", "Hello from integration test!", "authorId", 987654321L)
            );

            // When & Then
            MvcResult result = mockMvc.perform(post("/api/v1/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andReturn();

            String eventId = result.getResponse().getContentAsString();
            assertThat(eventId).startsWith("discord-");
            assertThat(eventId).hasSize(14); // "discord-" (8) + 6 chars
        }

        @Test
        @DisplayName("should create GitHub event and return 201")
        void shouldCreateGitHubEvent() throws Exception {
            // Given
            EventRequestDto request = new EventRequestDto(
                    "github",
                    "github.push_received",
                    Instant.now(),
                    "{\"source\": \"github\"}",
                    Map.of("user", "testuser", "repository", "my-repo", "branch", "main")
            );

            // When & Then
            MvcResult result = mockMvc.perform(post("/api/v1/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String eventId = result.getResponse().getContentAsString();
            assertThat(eventId).startsWith("github-");
        }

        @Test
        @DisplayName("should create Internal event and return 201")
        void shouldCreateInternalEvent() throws Exception {
            // Given
            EventRequestDto request = new EventRequestDto(
                    "internal",
                    "internal.scheduled_event",
                    Instant.now(),
                    "{\"source\": \"internal\"}",
                    Map.of("cronExpression", "0 0 * * *", "taskName", "daily-backup")
            );

            // When & Then
            MvcResult result = mockMvc.perform(post("/api/v1/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String eventId = result.getResponse().getContentAsString();
            assertThat(eventId).startsWith("internal-");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/events/{id}")
    class GetEvent {

        @Test
        @DisplayName("should retrieve created event")
        void shouldRetrieveCreatedEvent() throws Exception {
            // Given - Create an event first
            EventRequestDto request = new EventRequestDto(
                    "discord",
                    "discord.message_received",
                    Instant.parse("2026-01-04T12:30:00Z"),
                    "{\"source\": \"discord\"}",
                    Map.of("content", "Test message", "authorId", 111222333L)
            );

            MvcResult createResult = mockMvc.perform(post("/api/v1/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String eventId = createResult.getResponse().getContentAsString();

            // When & Then
            mockMvc.perform(get("/api/v1/events/{id}", eventId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(eventId))
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

    @Nested
    @DisplayName("Full flow")
    class FullFlow {

        @Test
        @DisplayName("should create, retrieve and verify event data integrity")
        void shouldCreateAndRetrieveWithDataIntegrity() throws Exception {
            // Given
            Instant timestamp = Instant.parse("2026-01-04T15:45:30Z");
            EventRequestDto request = new EventRequestDto(
                    "github",
                    "github.push_received",
                    timestamp,
                    "{\"source\": \"github\"}",
                    Map.of("user", "developer", "repository", "nexus", "branch", "feature/test")
            );

            // When - Create
            MvcResult createResult = mockMvc.perform(post("/api/v1/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String eventId = createResult.getResponse().getContentAsString();

            // When & Then - Retrieve and verify
            mockMvc.perform(get("/api/v1/events/{id}", eventId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(eventId))
                    .andExpect(jsonPath("$.source").value("github"))
                    .andExpect(jsonPath("$.type").value("github.push_received"))
                    .andExpect(jsonPath("$.payload.user").value("developer"))
                    .andExpect(jsonPath("$.payload.repository").value("nexus"))
                    .andExpect(jsonPath("$.payload.branch").value("feature/test"));
        }
    }
}