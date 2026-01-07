package fr.traqueur.nexus.core.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.traqueur.nexus.core.application.mapper.EventMapper;
import fr.traqueur.nexus.core.application.services.EventService;
import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.discord.DiscordContext;
import fr.traqueur.nexus.core.domain.events.discord.events.DiscordMessageReceived;
import fr.traqueur.nexus.core.infrastructure.config.RegistriesConfig;
import fr.traqueur.nexus.core.infrastructure.serialization.JacksonConfig;
import fr.traqueur.nexus.core.interfaces.rest.dto.EventResponseDto;
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
    private EventService eventService;

    @MockitoBean
    private EventMapper eventMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("GET /api/v1/events/{id}")
    class GetEvent {

        @Test
        @DisplayName("should return 200 with event when found")
        void shouldReturn200WithEvent() throws Exception {
            // Given
            String eventId = "discord-abc123";
            Event.Id id = new Event.Id("discord", "abc123");
            DiscordMessageReceived event = new DiscordMessageReceived(
                    id,
                    new DiscordContext(),
                    Instant.parse("2026-01-04T10:00:00Z"),
                    "Hello!",
                    123456789L
            );

            EventResponseDto responseDto = new EventResponseDto(
                    "discord-abc123",
                    "discord",
                    "discord.message_received",
                    Instant.parse("2026-01-04T10:00:00Z"),
                    "{\"source\": \"discord\"}",
                    Map.of("content", "Hello!", "authorId", 123456789)
            );

            when(eventService.findById(eventId)).thenReturn(Optional.of(event));
            when(eventMapper.toDto(event)).thenReturn(responseDto);

            // When & Then
            mockMvc.perform(get("/api/v1/events/{id}", eventId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("discord-abc123"))
                    .andExpect(jsonPath("$.source").value("discord"))
                    .andExpect(jsonPath("$.type").value("discord.message_received"))
                    .andExpect(jsonPath("$.payload.content").value("Hello!"))
                    .andExpect(jsonPath("$.payload.authorId").value(123456789));
        }

        @Test
        @DisplayName("should return 404 when event not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            String eventId = "unknown-abc123";
            when(eventService.findById(eventId)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/v1/events/{id}", eventId))
                    .andExpect(status().isNotFound());
        }
    }
}