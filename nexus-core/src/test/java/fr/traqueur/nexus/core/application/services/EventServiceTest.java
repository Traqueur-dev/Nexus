package fr.traqueur.nexus.core.application.services;

import fr.traqueur.nexus.core.TestFixtures;
import fr.traqueur.nexus.core.application.events.EventFactory;
import fr.traqueur.nexus.core.application.ports.in.IngestEventCommand;
import fr.traqueur.nexus.core.application.ports.out.EventRepository;
import fr.traqueur.nexus.core.application.registry.Registry;
import fr.traqueur.nexus.core.application.registry.UnknownTypeException;
import fr.traqueur.nexus.core.application.ports.out.WorkflowRepository;
import fr.traqueur.nexus.core.application.workflow.ActionDispatcher;
import fr.traqueur.nexus.core.application.workflow.WorkflowEngine;
import fr.traqueur.nexus.core.domain.events.EventType;
import fr.traqueur.nexus.core.domain.events.EventMetadata;
import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.discord.DiscordContext;
import fr.traqueur.nexus.core.domain.events.discord.events.DiscordMessageReceived;
import fr.traqueur.nexus.core.domain.events.github.GitHubContext;
import fr.traqueur.nexus.core.domain.events.github.events.GitHubPushReceived;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test for {@link EventService}.
 *
 * <p>No Spring context, no Testcontainers, no Docker: the service depends on the
 * {@link EventRepository} port, so a plain in-memory implementation is enough.
 * The integration test covering the real JPA adapter still exists — this one
 * covers the application logic in milliseconds instead of a container startup.
 */
@DisplayName("EventService")
class EventServiceTest {

    /** In-memory stand-in for the outbound port. */
    static class InMemoryEventRepository implements EventRepository {

        private final Map<Event.Id, Event> stored = new LinkedHashMap<>();

        @Override
        public void save(Event event) {
            stored.put(event.id(), event);
        }

        @Override
        public Optional<Event> findById(Event.Id id) {
            return Optional.ofNullable(stored.get(id));
        }

        @Override
        public Optional<Event> findLatestBySource(String source) {
            return stored.values().stream()
                    .filter(event -> event.context().source().equals(source))
                    .max(Comparator.comparing(Event::timestamp));
        }

        int size() {
            return stored.size();
        }
    }

    private InMemoryEventRepository repository;
    private EventService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEventRepository();
        WorkflowRepository noWorkflows = (EventType type) -> List.of();
        service = new EventService(repository, new EventFactory(TestFixtures.events()),
                new WorkflowEngine(noWorkflows, new ActionDispatcher(TestFixtures.actions(), List.of())));
    }

    private static DiscordMessageReceived discordEvent(String instance, Instant at, String content) {
        return new DiscordMessageReceived(
                new Event.Id("discord", instance), new DiscordContext(), at, content, 42L);
    }

    @Test
    @DisplayName("should store an event and read it back by id")
    void shouldStoreAndReadBack() {
        DiscordMessageReceived event = discordEvent("aaaaaa", Instant.parse("2026-01-04T10:00:00Z"), "hello");

        repository.save(event);

        assertThat(repository.size()).isEqualTo(1);
        assertThat(service.findById(event.id())).contains(event);
    }

    @Test
    @DisplayName("should return empty for an unknown id")
    void shouldReturnEmptyForUnknownId() {
        assertThat(service.findById(new Event.Id("discord", "zzzzzz"))).isEmpty();
    }

    @Test
    @DisplayName("should return the most recent event for a source")
    void shouldReturnMostRecentForSource() {
        DiscordMessageReceived older = discordEvent("aaaaaa", Instant.parse("2026-01-04T10:00:00Z"), "older");
        DiscordMessageReceived newer = discordEvent("bbbbbb", Instant.parse("2026-01-04T12:00:00Z"), "newer");

        repository.save(older);
        repository.save(newer);

        assertThat(service.findLatestBySource("discord")).contains(newer);
    }

    @Test
    @DisplayName("should not mix sources when looking up the latest event")
    void shouldNotMixSources() {
        DiscordMessageReceived discord = discordEvent("aaaaaa", Instant.parse("2026-01-04T10:00:00Z"), "discord");
        GitHubPushReceived github = new GitHubPushReceived(
                new Event.Id("github", "cccccc"), new GitHubContext(),
                Instant.parse("2026-01-04T18:00:00Z"), "someone", "nexus", "develop");

        repository.save(discord);
        repository.save(github);

        assertThat(service.findLatestBySource("discord")).contains(discord);
        assertThat(service.findLatestBySource("github")).contains(github);
    }

    @Test
    @DisplayName("should return empty for a source with no event")
    void shouldReturnEmptyForUnknownSource() {
        repository.save(discordEvent("aaaaaa", Instant.parse("2026-01-04T10:00:00Z"), "hello"));

        assertThat(service.findLatestBySource("minecraft")).isEmpty();
    }

    @Test
    @DisplayName("should build, identify and store an ingested event")
    void shouldIngestCommand() {
        IngestEventCommand command = new IngestEventCommand(
                "discord",
                "discord.message_received",
                Instant.parse("2026-01-04T10:00:00Z"),
                new DiscordContext(),
                Map.of("content", "ingested", "authorId", 7L));

        Event ingested = service.ingest(command).event();

        assertThat(ingested).isInstanceOf(DiscordMessageReceived.class);
        assertThat(ingested.id().prefix()).isEqualTo("discord");
        assertThat(((DiscordMessageReceived) ingested).content()).isEqualTo("ingested");
        assertThat(((DiscordMessageReceived) ingested).authorId()).isEqualTo(7L);
        assertThat(service.findById(ingested.id())).contains(ingested);
    }

    @Test
    @DisplayName("should give each ingested event its own identifier")
    void shouldGenerateDistinctIdentifiers() {
        IngestEventCommand command = new IngestEventCommand(
                "discord",
                "discord.message_received",
                Instant.parse("2026-01-04T10:00:00Z"),
                new DiscordContext(),
                Map.of("content", "same payload", "authorId", 7L));

        Event first = service.ingest(command).event();
        Event second = service.ingest(command).event();

        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(repository.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("should reject an unknown event type")
    void shouldRejectUnknownType() {
        IngestEventCommand command = new IngestEventCommand(
                "discord",
                "discord.not_a_real_type",
                Instant.parse("2026-01-04T10:00:00Z"),
                new DiscordContext(),
                Map.of());

        assertThatThrownBy(() -> service.ingest(command))
                .isInstanceOf(UnknownTypeException.class)
                .hasMessageContaining("discord.not_a_real_type");
        assertThat(repository.size()).isZero();
    }
}
