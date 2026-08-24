package fr.traqueur.nexus.core.application.services;

import fr.traqueur.nexus.core.application.ports.out.EventRepository;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
        service = new EventService(repository);
    }

    private static DiscordMessageReceived discordEvent(String instance, Instant at, String content) {
        return new DiscordMessageReceived(
                new Event.Id("discord", instance), new DiscordContext(), at, content, 42L);
    }

    @Test
    @DisplayName("should store an event and read it back by id")
    void shouldStoreAndReadBack() {
        DiscordMessageReceived event = discordEvent("aaaaaa", Instant.parse("2026-01-04T10:00:00Z"), "hello");

        service.save(event);

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

        service.save(older);
        service.save(newer);

        assertThat(service.findLatestBySource("discord")).contains(newer);
    }

    @Test
    @DisplayName("should not mix sources when looking up the latest event")
    void shouldNotMixSources() {
        DiscordMessageReceived discord = discordEvent("aaaaaa", Instant.parse("2026-01-04T10:00:00Z"), "discord");
        GitHubPushReceived github = new GitHubPushReceived(
                new Event.Id("github", "cccccc"), new GitHubContext(),
                Instant.parse("2026-01-04T18:00:00Z"), "someone", "nexus", "develop");

        service.save(discord);
        service.save(github);

        assertThat(service.findLatestBySource("discord")).contains(discord);
        assertThat(service.findLatestBySource("github")).contains(github);
    }

    @Test
    @DisplayName("should return empty for a source with no event")
    void shouldReturnEmptyForUnknownSource() {
        service.save(discordEvent("aaaaaa", Instant.parse("2026-01-04T10:00:00Z"), "hello"));

        assertThat(service.findLatestBySource("minecraft")).isEmpty();
    }
}
