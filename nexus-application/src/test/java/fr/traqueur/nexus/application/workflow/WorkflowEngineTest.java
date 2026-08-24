package fr.traqueur.nexus.application.workflow;

import fr.traqueur.nexus.application.ports.out.ActionHandler;
import fr.traqueur.nexus.application.ports.out.WorkflowRepository;
import fr.traqueur.nexus.domain.events.Event;
import fr.traqueur.nexus.domain.events.EventType;
import fr.traqueur.nexus.domain.events.discord.DiscordContext;
import fr.traqueur.nexus.domain.events.discord.events.DiscordMessageReceived;
import fr.traqueur.nexus.application.registry.Registries;
import fr.traqueur.nexus.domain.workflow.Action;
import fr.traqueur.nexus.domain.workflow.ActionMetadata;
import fr.traqueur.nexus.domain.workflow.Workflow;
import fr.traqueur.nexus.domain.workflow.conditions.AlwaysCondition;
import fr.traqueur.nexus.domain.workflow.conditions.EqualsCondition;
import fr.traqueur.nexus.domain.workflow.exceptions.ActionExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WorkflowEngine")
class WorkflowEngineTest {

    private static final EventType DISCORD_MESSAGE = EventType.of("discord.message_received");

    @ActionMetadata(type = "test.log")
    record LogAction(String tag) implements Action {
    }

    @ActionMetadata(type = "test.failing")
    record FailingAction(String tag) implements Action {
    }

    static class LogHandler implements ActionHandler<LogAction> {
        final List<String> executed = new ArrayList<>();

        @Override
        public Class<LogAction> handles() {
            return LogAction.class;
        }

        @Override
        public void execute(LogAction action, Event event) {
            executed.add(action.tag());
        }
    }

    static class FailingHandler implements ActionHandler<FailingAction> {
        @Override
        public Class<FailingAction> handles() {
            return FailingAction.class;
        }

        @Override
        public void execute(FailingAction action, Event event) throws ActionExecutionException {
            throw new ActionExecutionException("boom: " + action.tag());
        }
    }

    private static Event event(String content) {
        return new DiscordMessageReceived(
                new Event.Id("discord", "abc123"), new DiscordContext(),
                Instant.parse("2026-01-04T10:00:00Z"), content, 1L);
    }

    @SafeVarargs
    private static ActionDispatcher dispatcher(ActionHandler<? extends Action>... handlers) {
        return new ActionDispatcher(
                Registries.actions().register(LogAction.class).register(FailingAction.class),
                List.of(handlers));
    }

    private static WorkflowRepository containing(Workflow... workflows) {
        List<Workflow> all = List.of(workflows);
        return type -> all.stream().filter(workflow -> workflow.triggersOn(type)).toList();
    }

    @Nested
    @DisplayName("matching")
    class Matching {

        @Test
        @DisplayName("should run the actions of a matching workflow")
        void shouldRunMatchingWorkflow() {
            LogHandler handler = new LogHandler();
            Workflow workflow = new Workflow("wf-1", List.of(DISCORD_MESSAGE),
                    new AlwaysCondition(), List.of(new LogAction("a"), new LogAction("b")));
            WorkflowEngine engine = new WorkflowEngine(containing(workflow), dispatcher(handler));

            List<WorkflowRun> runs = engine.run(DISCORD_MESSAGE, event("hello"));

            assertThat(handler.executed).containsExactly("a", "b");
            assertThat(runs).hasSize(1);
            assertThat(runs.getFirst().succeeded()).isTrue();
            assertThat(runs.getFirst().workflowId()).isEqualTo("wf-1");
        }

        @Test
        @DisplayName("should not run a workflow whose condition does not hold")
        void shouldSkipNonMatchingCondition() {
            LogHandler handler = new LogHandler();
            Workflow workflow = new Workflow("wf-1", List.of(DISCORD_MESSAGE),
                    new EqualsCondition("content", "expected"), List.of(new LogAction("a")));
            WorkflowEngine engine = new WorkflowEngine(containing(workflow), dispatcher(handler));

            List<WorkflowRun> runs = engine.run(DISCORD_MESSAGE, event("something else"));

            assertThat(handler.executed).isEmpty();
            assertThat(runs).isEmpty();
        }

        @Test
        @DisplayName("should report nothing when no workflow reacts to the type")
        void shouldReportNothingWhenNoWorkflow() {
            WorkflowEngine engine = new WorkflowEngine(containing(), dispatcher());

            assertThat(engine.run(DISCORD_MESSAGE, event("hello"))).isEmpty();
        }
    }

    @Nested
    @DisplayName("failure handling")
    class FailureHandling {

        @Test
        @DisplayName("should keep running the remaining actions after one fails")
        void shouldContinueAfterFailingAction() {
            LogHandler logHandler = new LogHandler();
            Workflow workflow = new Workflow("wf-1", List.of(DISCORD_MESSAGE), new AlwaysCondition(),
                    List.of(new LogAction("before"), new FailingAction("x"), new LogAction("after")));
            WorkflowEngine engine = new WorkflowEngine(containing(workflow),
                    dispatcher(logHandler, new FailingHandler()));

            List<WorkflowRun> runs = engine.run(DISCORD_MESSAGE, event("hello"));

            // The actions of a workflow are separate intents, not a transaction.
            assertThat(logHandler.executed).containsExactly("before", "after");
            assertThat(runs.getFirst().succeeded()).isFalse();
            assertThat(runs.getFirst().failures()).hasSize(1);
            assertThat(runs.getFirst().failures().getFirst().failure()).contains("boom: x");
        }

        @Test
        @DisplayName("should keep running other workflows when one cannot be evaluated")
        void shouldIsolateBrokenCondition() {
            LogHandler handler = new LogHandler();
            Workflow broken = new Workflow("wf-broken", List.of(DISCORD_MESSAGE),
                    new EqualsCondition("no_such_field", "x"), List.of(new LogAction("never")));
            Workflow healthy = new Workflow("wf-ok", List.of(DISCORD_MESSAGE),
                    new AlwaysCondition(), List.of(new LogAction("ran")));
            WorkflowEngine engine = new WorkflowEngine(containing(broken, healthy),
                    dispatcher(handler));

            List<WorkflowRun> runs = engine.run(DISCORD_MESSAGE, event("hello"));

            assertThat(handler.executed).containsExactly("ran");
            assertThat(runs).hasSize(2);
            WorkflowRun brokenRun = runs.stream().filter(r -> r.workflowId().equals("wf-broken")).findFirst().orElseThrow();
            assertThat(brokenRun.fired()).isFalse();
            assertThat(brokenRun.evaluationError()).contains("no_such_field");
            assertThat(brokenRun.succeeded()).isFalse();
        }

        @Test
        @DisplayName("should record a missing handler as a failed action, not a crash")
        void shouldRecordMissingHandler() {
            Workflow workflow = new Workflow("wf-1", List.of(DISCORD_MESSAGE),
                    new AlwaysCondition(), List.of(new LogAction("a")));
            WorkflowEngine engine = new WorkflowEngine(containing(workflow), dispatcher());

            List<WorkflowRun> runs = engine.run(DISCORD_MESSAGE, event("hello"));

            assertThat(runs.getFirst().succeeded()).isFalse();
            assertThat(runs.getFirst().failures().getFirst().failure()).contains("No handler registered");
        }
    }
}
