package fr.traqueur.nexus.core.application.workflow;

import fr.traqueur.nexus.core.application.ports.out.ActionHandler;
import fr.traqueur.nexus.core.domain.events.Event;
import fr.traqueur.nexus.core.domain.events.discord.DiscordContext;
import fr.traqueur.nexus.core.domain.events.discord.events.DiscordMessageReceived;
import fr.traqueur.nexus.core.domain.workflow.Action;
import fr.traqueur.nexus.core.domain.workflow.actions.SendEmailAction;
import fr.traqueur.nexus.core.domain.workflow.exceptions.ActionExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ActionDispatcher")
class ActionDispatcherTest {

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

    private static final Event EVENT = new DiscordMessageReceived(
            new Event.Id("discord", "abc123"), new DiscordContext(),
            Instant.parse("2026-01-04T10:00:00Z"), "hello", 1L);

    @Test
    @DisplayName("should route an action to the handler registered for its type")
    void shouldRouteToHandler() throws ActionExecutionException {
        RecordingHandler handler = new RecordingHandler();
        ActionDispatcher dispatcher = new ActionDispatcher(List.of(handler));

        dispatcher.dispatch(new LogAction("hi"), EVENT);

        assertThat(handler.executed).containsExactly(new LogAction("hi"));
    }

    @Test
    @DisplayName("should fail explicitly when no handler is registered")
    void shouldFailWithoutHandler() {
        ActionDispatcher dispatcher = new ActionDispatcher(List.of(new RecordingHandler()));

        assertThatThrownBy(() -> dispatcher.dispatch(new SendEmailAction("s", "c", "to@x"), EVENT))
                .isInstanceOf(ActionExecutionException.class)
                .hasMessageContaining("No handler registered")
                .hasMessageContaining("LogAction");
    }

    @Test
    @DisplayName("should report an empty registry rather than a bare null")
    void shouldReportEmptyRegistry() {
        ActionDispatcher dispatcher = new ActionDispatcher(List.of());

        assertThatThrownBy(() -> dispatcher.dispatch(new LogAction("hi"), EVENT))
                .isInstanceOf(ActionExecutionException.class)
                .hasMessageContaining("<none>");
    }

    @Test
    @DisplayName("should reject two handlers claiming the same action type")
    void shouldRejectDuplicateHandlers() {
        assertThatThrownBy(() -> new ActionDispatcher(List.of(new RecordingHandler(), new RecordingHandler())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Two handlers registered");
    }

    @Test
    @DisplayName("canHandle should reflect what is registered")
    void canHandleShouldReflectRegistration() {
        ActionDispatcher dispatcher = new ActionDispatcher(List.of(new RecordingHandler()));

        assertThat(dispatcher.canHandle(new LogAction("hi"))).isTrue();
        assertThat(dispatcher.canHandle(new SendEmailAction("s", "c", "to@x"))).isFalse();
    }
}
