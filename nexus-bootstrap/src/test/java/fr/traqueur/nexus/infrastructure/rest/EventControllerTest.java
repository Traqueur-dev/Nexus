package fr.traqueur.nexus.infrastructure.rest;

import tools.jackson.databind.ObjectMapper;

import fr.traqueur.nexus.application.ports.in.QueryEvents;
import fr.traqueur.nexus.domain.events.Event;
import fr.traqueur.nexus.domain.events.discord.DiscordContext;
import fr.traqueur.nexus.domain.events.discord.events.DiscordMessageReceived;
import fr.traqueur.nexus.bootstrap.RegistriesConfig;
import fr.traqueur.nexus.infrastructure.serialization.JacksonConfig;
import fr.traqueur.nexus.infrastructure.rest.dto.EventResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import({JacksonConfig.class, RegistriesConfig.class})
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueryEvents events;

    @MockitoBean
    private EventDtoMapper eventDtoMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("GET /api/v1/events/{id}")
    class GetEvent {

        @Test
        @DisplayName("should return 200 with event when found")
        void shouldReturn200WithEvent() throws Exception {
            // Given
            Event.Id id = Event.Id.generate("discord");
            String eventId = id.toString();
            DiscordMessageReceived event = new DiscordMessageReceived(
                    id,
                    new DiscordContext(),
                    Instant.parse("2026-01-04T10:00:00Z"),
                    "Hello!",
                    123456789L
            );

            EventResponseDto responseDto = new EventResponseDto(
                    eventId,
                    "discord",
                    "discord.message_received",
                    Instant.parse("2026-01-04T10:00:00Z"),
                    new DiscordContext(),
                    Map.of("content", "Hello!", "authorId", 123456789)
            );

            when(events.findById(id)).thenReturn(Optional.of(event));
            when(eventDtoMapper.toDto(event)).thenReturn(responseDto);

            // When & Then
            mockMvc.perform(get("/api/v1/events/{id}", eventId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(eventId))
                    .andExpect(jsonPath("$.source").value("discord"))
                    .andExpect(jsonPath("$.type").value("discord.message_received"))
                    // An object, not an escaped string: jsonPath resolves through it
                    // only because the converter wrote the context itself (#32).
                    .andExpect(jsonPath("$.context.source").value("discord"))
                    .andExpect(jsonPath("$.payload.content").value("Hello!"))
                    .andExpect(jsonPath("$.payload.authorId").value(123456789));
        }

        @Test
        @DisplayName("should return 404 when event not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            Event.Id missing = Event.Id.generate("unknown");
            String eventId = missing.toString();
            when(events.findById(missing)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/v1/events/{id}", eventId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when the id is malformed")
        void shouldReturn400WhenIdMalformed() throws Exception {
            // A malformed id never reaches the service: it is rejected while parsing.
            mockMvc.perform(get("/api/v1/events/{id}", "not-a-valid-id"))
                    .andExpect(status().isBadRequest());
        }
    }
}