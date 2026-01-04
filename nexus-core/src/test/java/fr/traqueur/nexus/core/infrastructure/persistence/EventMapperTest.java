package fr.traqueur.nexus.core.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.traqueur.nexus.core.application.mapper.EventMapper;
import fr.traqueur.nexus.core.domain.events.Context;
import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.discord.DiscordContext;
import fr.traqueur.nexus.core.domain.events.discord.events.DiscordMessageReceived;
import fr.traqueur.nexus.core.domain.events.github.GitHubContext;
import fr.traqueur.nexus.core.domain.events.github.events.GitHubPushReceived;
import fr.traqueur.nexus.core.domain.events.internal.InternalContext;
import fr.traqueur.nexus.core.domain.events.internal.events.ScheduledEvent;
import fr.traqueur.nexus.core.infrastructure.persistence.entities.EventEntity;
import fr.traqueur.nexus.core.infrastructure.registry.EventRegistry;
import fr.traqueur.nexus.core.infrastructure.serialization.ContextMixin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class EventMapperTest {

    private EventMapper mapper;

    @BeforeEach
    void setUp() {
        EventRegistry registry = new EventRegistry();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.addMixIn(Context.class, ContextMixin.class);
        objectMapper.findAndRegisterModules(); // For Instant support
        mapper = new EventMapper(registry, objectMapper);
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("should convert DiscordMessageReceived to entity")
        void shouldConvertDiscordEvent() {
            Event.Id id = new Event.Id("disc", "abc123");
            DiscordContext context = new DiscordContext();
            Instant timestamp = Instant.parse("2026-01-03T10:00:00Z");
            DiscordMessageReceived event = new DiscordMessageReceived(
                    id, context, timestamp, "Hello world!", 123456789L
            );

            EventEntity entity = mapper.toEntity(event);

            assertThat(entity.getId()).isEqualTo("disc-abc123");
            assertThat(entity.getSource()).isEqualTo("discord");
            assertThat(entity.getType()).isEqualTo("discord.message_received");
            assertThat(entity.getTimestamp()).isEqualTo(timestamp);
            assertThat(entity.getContext()).contains("discord");
            assertThat(entity.getPayload()).contains("Hello world!");
            assertThat(entity.getPayload()).contains("123456789");
        }

        @Test
        @DisplayName("should convert GitHubPushReceived to entity")
        void shouldConvertGitHubEvent() {
            Event.Id id = new Event.Id("gh", "xyz789");
            GitHubContext context = new GitHubContext();
            Instant timestamp = Instant.now();
            GitHubPushReceived event = new GitHubPushReceived(
                    id, context, timestamp, "octocat", "nexus", "main"
            );

            EventEntity entity = mapper.toEntity(event);

            assertThat(entity.getId()).isEqualTo("gh-xyz789");
            assertThat(entity.getSource()).isEqualTo("github");
            assertThat(entity.getType()).isEqualTo("github.push_received");
            assertThat(entity.getPayload()).contains("octocat");
            assertThat(entity.getPayload()).contains("nexus");
            assertThat(entity.getPayload()).contains("main");
        }

        @Test
        @DisplayName("should convert ScheduledEvent to entity")
        void shouldConvertInternalEvent() {
            Event.Id id = new Event.Id("int", "def456");
            InternalContext context = new InternalContext();
            Instant timestamp = Instant.now();
            ScheduledEvent event = new ScheduledEvent(
                    id, context, timestamp, "0 0 * * *", "daily-backup"
            );

            EventEntity entity = mapper.toEntity(event);

            assertThat(entity.getId()).isEqualTo("int-def456");
            assertThat(entity.getSource()).isEqualTo("internal");
            assertThat(entity.getType()).isEqualTo("internal.scheduled_event");
            assertThat(entity.getPayload()).contains("0 0 * * *");
            assertThat(entity.getPayload()).contains("daily-backup");
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("should convert entity to DiscordMessageReceived")
        void shouldConvertToDiscordEvent() {
            EventEntity entity = new EventEntity();
            entity.setId("disc-abc123");
            entity.setSource("discord");
            entity.setType("discord.message_received");
            entity.setTimestamp(Instant.parse("2026-01-03T10:00:00Z"));
            entity.setContext("{\"source\":\"discord\"}");
            entity.setPayload("{\"content\":\"Hello world!\",\"authorId\":123456789}");

            Event event = mapper.toDomain(entity);

            assertThat(event).isInstanceOf(DiscordMessageReceived.class);
            DiscordMessageReceived discordEvent = (DiscordMessageReceived) event;
            assertThat(discordEvent.id().prefix()).isEqualTo("disc");
            assertThat(discordEvent.id().instance()).isEqualTo("abc123");
            assertThat(discordEvent.context()).isInstanceOf(DiscordContext.class);
            assertThat(discordEvent.content()).isEqualTo("Hello world!");
            assertThat(discordEvent.authorId()).isEqualTo(123456789L);
        }

        @Test
        @DisplayName("should convert entity to GitHubPushReceived")
        void shouldConvertToGitHubEvent() {
            EventEntity entity = new EventEntity();
            entity.setId("gh-xyz789");
            entity.setSource("github");
            entity.setType("github.push_received");
            entity.setTimestamp(Instant.now());
            entity.setContext("{\"source\":\"github\"}");
            entity.setPayload("{\"user\":\"octocat\",\"repository\":\"nexus\",\"branch\":\"main\"}");

            Event event = mapper.toDomain(entity);

            assertThat(event).isInstanceOf(GitHubPushReceived.class);
            GitHubPushReceived gitHubEvent = (GitHubPushReceived) event;
            assertThat(gitHubEvent.user()).isEqualTo("octocat");
            assertThat(gitHubEvent.repository()).isEqualTo("nexus");
            assertThat(gitHubEvent.branch()).isEqualTo("main");
        }

        @Test
        @DisplayName("should convert entity to ScheduledEvent")
        void shouldConvertToInternalEvent() {
            EventEntity entity = new EventEntity();
            entity.setId("int-def456");
            entity.setSource("internal");
            entity.setType("internal.scheduled_event");
            entity.setTimestamp(Instant.now());
            entity.setContext("{\"source\":\"internal\"}");
            entity.setPayload("{\"cronExpression\":\"0 0 * * *\",\"taskName\":\"daily-backup\"}");

            Event event = mapper.toDomain(entity);

            assertThat(event).isInstanceOf(ScheduledEvent.class);
            ScheduledEvent scheduledEvent = (ScheduledEvent) event;
            assertThat(scheduledEvent.cronExpression()).isEqualTo("0 0 * * *");
            assertThat(scheduledEvent.taskName()).isEqualTo("daily-backup");
        }
    }

    @Nested
    @DisplayName("roundtrip")
    class Roundtrip {

        @Test
        @DisplayName("should preserve DiscordMessageReceived through roundtrip")
        void shouldPreserveDiscordEvent() {
            Event.Id id = new Event.Id("disc", "abc123");
            DiscordContext context = new DiscordContext();
            Instant timestamp = Instant.parse("2026-01-03T10:00:00Z");
            DiscordMessageReceived original = new DiscordMessageReceived(
                    id, context, timestamp, "Hello world!", 123456789L
            );

            EventEntity entity = mapper.toEntity(original);
            Event restored = mapper.toDomain(entity);

            assertThat(restored).isInstanceOf(DiscordMessageReceived.class);
            DiscordMessageReceived restoredEvent = (DiscordMessageReceived) restored;
            assertThat(restoredEvent.id().toString()).isEqualTo(original.id().toString());
            assertThat(restoredEvent.timestamp()).isEqualTo(original.timestamp());
            assertThat(restoredEvent.content()).isEqualTo(original.content());
            assertThat(restoredEvent.authorId()).isEqualTo(original.authorId());
        }

        @Test
        @DisplayName("should preserve GitHubPushReceived through roundtrip")
        void shouldPreserveGitHubEvent() {
            Event.Id id = new Event.Id("gh", "xyz789");
            GitHubContext context = new GitHubContext();
            Instant timestamp = Instant.now();
            GitHubPushReceived original = new GitHubPushReceived(
                    id, context, timestamp, "octocat", "nexus", "main"
            );

            EventEntity entity = mapper.toEntity(original);
            Event restored = mapper.toDomain(entity);

            assertThat(restored).isInstanceOf(GitHubPushReceived.class);
            GitHubPushReceived restoredEvent = (GitHubPushReceived) restored;
            assertThat(restoredEvent.user()).isEqualTo(original.user());
            assertThat(restoredEvent.repository()).isEqualTo(original.repository());
            assertThat(restoredEvent.branch()).isEqualTo(original.branch());
        }
    }
}