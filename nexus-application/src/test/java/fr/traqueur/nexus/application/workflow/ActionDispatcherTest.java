package fr.traqueur.nexus.application.workflow;

import fr.traqueur.nexus.application.ports.out.ActionHandler;
import fr.traqueur.nexus.domain.events.Event;
import fr.traqueur.nexus.domain.events.discord.DiscordContext;
import fr.traqueur.nexus.domain.events.discord.events.DiscordMessageReceived;
import fr.traqueur.nexus.application.registry.Registries;
import fr.traqueur.nexus.application.registry.UnknownTypeException;
import fr.traqueur.nexus.domain.workflow.Action;
import fr.traqueur.nexus.domain.workflow.ActionMetadata;
import fr.traqueur.nexus.domain.workflow.actions.SendEmailAction;
import fr.traqueur.nexus.domain.workflow.exceptions.ActionExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ActionDispatcher")
class ActionDispatcherTest {

    // Declared outside the domain, like a plugin's action would be.
    @ActionMetadata(type = "test.log")
    record LogAction(String message) implements Action {
    }

    static class RecordingHandler implements ActionHandler<LogAction> {
        final List<LogAction> executed = new ArrayList<>();

        @Override
        public Class<LogAction> handles() {
            return LogAction.class;
        }

        @Override
        public void execute(LogAction action, Event event) {
            executed.add(action);
        }
    }

    @SafeVarargs
    private static ActionDispatcher dispatcher(ActionHandler<? extends Action>... handlers) {
        return new ActionDispatcher(Registries.actions().register(LogAction.class), List.of(handlers));
    }

    private static final Event EVENT = new DiscordMessageReceived(
            new Event.Id("discord", "abc123"), new DiscordContext(),
            Instant.parse("2026-01-04T10:00:00Z"), "hello", 1L);

    @Test
    @DisplayName("should route an action to the handler registered for its type")
    void shouldRouteToHandler() throws ActionExecutionException {
        RecordingHandler handler = new RecordingHandler();
        ActionDispatcher dispatcher = dispatcher(handler);

        dispatcher.dispatch(new LogAction("hi"), EVENT);

        assertThat(handler.executed).containsExactly(new LogAction("hi"));
    }

    @Test
    @DisplayName("should fail explicitly when no handler is registered")
    void shouldFailWithoutHandler() {
        ActionDispatcher dispatcher = dispatcher(new RecordingHandler());

        assertThatThrownBy(() -> dispatcher.dispatch(new SendEmailAction("s", "c", "to@x"), EVENT))
                .isInstanceOf(ActionExecutionException.class)
                .hasMessageContaining("No handler registered")
                .hasMessageContaining("test.log");
    }

    @Test
    @DisplayName("should report an empty registry rather than a bare null")
    void shouldReportEmptyRegistry() {
        ActionDispatcher dispatcher = dispatcher();

        assertThatThrownBy(() -> dispatcher.dispatch(new LogAction("hi"), EVENT))
                .isInstanceOf(ActionExecutionException.class)
                .hasMessageContaining("<none>");
    }

    @Test
    @DisplayName("should fail clearly when the action type was never registered")
    void shouldFailForUnregisteredActionType() {
        record UnregisteredAction() implements Action {
        }

        ActionDispatcher dispatcher = dispatcher(new RecordingHandler());

        assertThatThrownBy(() -> dispatcher.dispatch(new UnregisteredAction(), EVENT))
                .isInstanceOf(ActionExecutionException.class)
                .hasMessageContaining("not registered")
                .hasMessageContaining("UnregisteredAction");
    }

    @Test
    @DisplayName("should refuse a handler for an unregistered action type at startup")
    void shouldRefuseHandlerForUnregisteredType() {
        record OrphanAction() implements Action {
        }
        class OrphanHandler implements ActionHandler<OrphanAction> {
            @Override
            public Class<OrphanAction> handles() {
                return OrphanAction.class;
            }

            @Override
            public void execute(OrphanAction action, Event event) {
            }
        }

        // Failing at startup beats failing on the first event that needs it.
        assertThatThrownBy(() -> dispatcher(new OrphanHandler()))
                .isInstanceOf(UnknownTypeException.class);
    }

    @Test
    @DisplayName("should reject two handlers claiming the same action type")
    void shouldRejectDuplicateHandlers() {
        assertThatThrownBy(() -> dispatcher(new RecordingHandler(), new RecordingHandler()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Two handlers registered");
    }

    @Test
    @DisplayName("should dispatch a registered type and refuse an unregistered one")
    void shouldDispatchRegisteredAndRefuseUnregistered() {
        RecordingHandler handler = new RecordingHandler();
        ActionDispatcher dispatcher = dispatcher(handler);

        assertThatCode(() -> dispatcher.dispatch(new LogAction("hi"), EVENT)).doesNotThrowAnyException();
        assertThatThrownBy(() -> dispatcher.dispatch(new SendEmailAction("s", "c", "to@x"), EVENT))
                .isInstanceOf(ActionExecutionException.class);
    }
}
