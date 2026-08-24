package fr.traqueur.nexus.core.bootstrap;

import fr.traqueur.nexus.core.application.events.EventFactory;
import fr.traqueur.nexus.core.application.ports.out.ActionHandler;
import fr.traqueur.nexus.core.application.ports.out.EventRepository;
import fr.traqueur.nexus.core.application.ports.out.WorkflowRepository;
import fr.traqueur.nexus.core.application.registry.Registry;
import fr.traqueur.nexus.core.application.services.EventService;
import fr.traqueur.nexus.core.application.workflow.ActionDispatcher;
import fr.traqueur.nexus.core.application.workflow.WorkflowEngine;
import fr.traqueur.nexus.core.TestFixtures;
import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.discord.DiscordContext;
import fr.traqueur.nexus.core.domain.events.discord.events.DiscordMessageReceived;
import fr.traqueur.nexus.core.domain.events.EventMetadata;
import fr.traqueur.nexus.core.domain.workflow.actions.SendEmailAction;
import fr.traqueur.nexus.core.domain.workflow.exceptions.ActionExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the application layer wires up.
 *
 * <p>Uses {@link ApplicationContextRunner}, so it exercises real Spring wiring
 * without a database, a broker or Docker — which is what makes it worth running
 * on every build rather than only in the integration suite.
 */
@DisplayName("ApplicationConfig")
class ApplicationConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ApplicationConfig.class)
            .withBean("eventRegistry", Registry.class, TestFixtures::events)
            .withBean("actionRegistry", Registry.class, TestFixtures::actions)
            .withBean(EventRepository.class, StubEventRepository::new)
            .withBean(WorkflowRepository.class, () -> type -> List.of());

    private static final Event EVENT = new DiscordMessageReceived(
            new Event.Id("discord", "abc123"), new DiscordContext(),
            java.time.Instant.parse("2026-01-04T10:00:00Z"), "hello", 1L);

    static class StubEventRepository implements EventRepository {
        @Override
        public void save(Event event) {
        }

        @Override
        public Optional<Event> findById(Event.Id id) {
            return Optional.empty();
        }

        @Override
        public Optional<Event> findLatestBySource(String source) {
            return Optional.empty();
        }
    }

    static class StubEmailHandler implements ActionHandler<SendEmailAction> {
        @Override
        public Class<SendEmailAction> handles() {
            return SendEmailAction.class;
        }

        @Override
        public void execute(SendEmailAction action, Event event) throws ActionExecutionException {
        }
    }

    @Test
    @DisplayName("should start with no action handler at all")
    void shouldStartWithoutAnyActionHandler() {
        // SendEmailActionHandler only exists when SMTP is configured, so zero
        // handlers is a legitimate state. Injecting a plain List<ActionHandler>
        // would fail the context here: Spring requires at least one candidate.
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ActionDispatcher.class);
            assertThatThrownBy(() -> context.getBean(ActionDispatcher.class)
                    .dispatch(new SendEmailAction("s", "c", "to@example.com"), EVENT))
                    .isInstanceOf(ActionExecutionException.class)
                    .hasMessageContaining("No handler registered");
        });
    }

    @Test
    @DisplayName("should pick up an action handler when one is present")
    void shouldPickUpActionHandler() {
        runner.withBean(ActionHandler.class, StubEmailHandler::new).run(context -> {
            assertThat(context).hasNotFailed();
            assertThatCode(() -> context.getBean(ActionDispatcher.class)
                    .dispatch(new SendEmailAction("s", "c", "to@example.com"), EVENT))
                    .doesNotThrowAnyException();
        });
    }

    @Test
    @DisplayName("should expose the application beans")
    void shouldExposeApplicationBeans() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(EventFactory.class);
            assertThat(context).hasSingleBean(WorkflowEngine.class);
            assertThat(context).hasSingleBean(EventService.class);
        });
    }
}
