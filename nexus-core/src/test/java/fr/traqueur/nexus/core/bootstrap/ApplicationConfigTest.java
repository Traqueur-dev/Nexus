package fr.traqueur.nexus.core.bootstrap;

import fr.traqueur.nexus.core.application.events.EventFactory;
import fr.traqueur.nexus.core.application.ports.out.ActionHandler;
import fr.traqueur.nexus.core.application.ports.out.EventRepository;
import fr.traqueur.nexus.core.application.ports.out.WorkflowRepository;
import fr.traqueur.nexus.core.application.registry.Registry;
import fr.traqueur.nexus.core.application.services.EventService;
import fr.traqueur.nexus.core.application.workflow.ActionDispatcher;
import fr.traqueur.nexus.core.application.workflow.WorkflowEngine;
import fr.traqueur.nexus.core.domain.events.CoreEvents;
import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.EventMetadata;
import fr.traqueur.nexus.core.domain.workflow.actions.SendEmailAction;
import fr.traqueur.nexus.core.domain.workflow.exceptions.ActionExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
            .withBean(Registry.class, () -> new Registry<>(Event.class, EventMetadata.class, EventMetadata::type)
                    .registerAll(CoreEvents.types()))
            .withBean(EventRepository.class, StubEventRepository::new)
            .withBean(WorkflowRepository.class, () -> type -> List.of());

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
            assertThat(context.getBean(ActionDispatcher.class)
                    .canHandle(new SendEmailAction("s", "c", "to@example.com"))).isFalse();
        });
    }

    @Test
    @DisplayName("should pick up an action handler when one is present")
    void shouldPickUpActionHandler() {
        runner.withBean(ActionHandler.class, StubEmailHandler::new).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ActionDispatcher.class)
                    .canHandle(new SendEmailAction("s", "c", "to@example.com"))).isTrue();
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
